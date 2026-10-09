package com.stickeria.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.view.*
import android.widget.*
import android.webkit.*
import org.json.JSONObject
import java.net.*
import java.io.*

class MainActivity : Activity() {
 private val ui=Handler(Looper.getMainLooper())
 private val imagePool=java.util.concurrent.Executors.newFixedThreadPool(4)
 private lateinit var list:GridLayout
 private lateinit var info:TextView
 private lateinit var query:EditText
 private lateinit var source:Spinner
 private val names=arrayOf("Todas las fuentes","Wikimedia Commons","Openverse","OpenMoji (emojis)","Telegram (packs públicos)","GIPHY (API)","Tenor (API)","Pinterest (web)","Instagram (web)","Google Imágenes","Bing Imágenes")
 private data class Item(val title:String,val thumb:String,val url:String,val origin:String)
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val bg=Color.rgb(13,15,29);val panel=Color.rgb(27,30,49);val purple=Color.rgb(124,82,245);val muted=Color.rgb(169,174,199)
  window.statusBarColor=bg;window.navigationBarColor=bg
  fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
  fun shape(color:Int,radius:Int=16)=android.graphics.drawable.GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
  fun title(t:String,size:Float=15f,color:Int=Color.WHITE)=TextView(this).apply{text=t;textSize=size;setTextColor(color);setTypeface(null,1)}
  fun button(t:String,color:Int,onTap:()->Unit)=TextView(this).apply{
   text=t;textSize=14f;setTypeface(null,1);gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=shape(color,15);setOnClickListener{onTap()}
  }
  val page=LinearLayout(this).apply{orientation=1;setBackgroundColor(bg);setPadding(dp(18),dp(12),dp(18),dp(8))}
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  val brand=LinearLayout(this).apply{orientation=1}
  brand.addView(title("✦ StickerIA",27f))
  brand.addView(TextView(this).apply{text="Encontrá tu próxima reacción";textSize=12f;setTextColor(muted)})
  header.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
  header.addView(button("♛ PRO",Color.rgb(75,52,133)){AlertDialog.Builder(this).setTitle("StickerIA Premium").setMessage("Próximamente: sin anuncios y más búsquedas con IA. Todavía no se realizan cobros.").setPositiveButton("Entendido",null).show()},LinearLayout.LayoutParams(dp(78),dp(38)))
  page.addView(header)
  val searchCard=LinearLayout(this).apply{orientation=1;background=shape(panel,22);setPadding(dp(14),dp(14),dp(14),dp(14))}
  searchCard.addView(title("¿Qué sticker estás buscando?",18f))
  searchCard.addView(TextView(this).apply{text="Memes, animales, reacciones y mucho más";textSize=12f;setTextColor(muted);setPadding(0,dp(3),0,dp(12))})
  query=EditText(this).apply{hint="Ej: perros graciosos, memes…";setSingleLine(true);textSize=15f;setPadding(dp(14),0,dp(14),0);background=shape(Color.rgb(43,46,69),13);setTextColor(Color.WHITE);setHintTextColor(muted)}
  searchCard.addView(query,LinearLayout.LayoutParams(-1,dp(52)))
  val quick=LinearLayout(this).apply{orientation=0;setPadding(0,dp(12),0,0)}
  quick.addView(button("✦ Explorar",purple){search()},LinearLayout.LayoutParams(0,dp(48),1f))
  quick.addView(button("＋ Crear sticker",Color.rgb(58,63,89)){startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*"},17)},LinearLayout.LayoutParams(0,dp(48),1f).apply{leftMargin=dp(8)})
  searchCard.addView(quick)
  searchCard.addView(button("🌐 Buscar en Internet",Color.rgb(58,63,89)){openWebSearch()},LinearLayout.LayoutParams(-1,dp(44)).apply{topMargin=dp(9)})
  page.addView(searchCard,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)})
  val categories=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false}
  val chips=LinearLayout(this).apply{orientation=0;setPadding(0,dp(12),0,dp(12))}
  listOf("😂 Memes","🐶 Perros","🐱 Gatos","❤️ Amor","🎉 Festejos").forEach{label->
   chips.addView(button(label,Color.rgb(44,47,71)){query.setText(label.substringAfter(" "));search()},LinearLayout.LayoutParams(dp(104),dp(40)).apply{rightMargin=dp(8)})
  }
  categories.addView(chips);page.addView(categories)
  val controls=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL}
  controls.addView(title("Descubrir stickers",18f),LinearLayout.LayoutParams(0,-2,1f))
  controls.addView(button("⚙ Fuentes",Color.rgb(57,61,87)){showSources()},LinearLayout.LayoutParams(dp(104),dp(40)))
  page.addView(controls)
  source=Spinner(this)
  source.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  source.visibility=View.GONE
  page.addView(source,LinearLayout.LayoutParams(1,1))
  info=TextView(this).apply{text="Elegí una categoría o escribí algo para explorar.";textSize=12f;setTextColor(muted);setPadding(0,dp(10),0,dp(10))}
  page.addView(info)
  val scroll=ScrollView(this).apply{isFillViewport=false}
  list=GridLayout(this).apply{columnCount=3;alignmentMode=GridLayout.ALIGN_BOUNDS}
  scroll.addView(list)
  page.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val nav=LinearLayout(this).apply{orientation=0;setPadding(0,dp(8),0,dp(4))}
  nav.addView(button("⌕ Buscar",purple){query.requestFocus()},LinearLayout.LayoutParams(0,dp(49),1f))
  nav.addView(button("▣ Mis packs",Color.rgb(48,52,78)){showMyPack()},LinearLayout.LayoutParams(0,dp(49),1f).apply{leftMargin=dp(8)})
  nav.addView(button("⚙ Ajustes",Color.rgb(48,52,78)){showSettings()},LinearLayout.LayoutParams(0,dp(49),1f).apply{leftMargin=dp(8)})
  page.addView(nav)
  page.setOnApplyWindowInsetsListener { view, insets ->
   val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars())
   view.setPadding(dp(18),bars.top+dp(12),dp(18),bars.bottom+dp(8))
   insets
  }
  setContentView(page)
 }
 private fun showSources(){
  AlertDialog.Builder(this).setTitle("Elegir fuente").setSingleChoiceItems(names,source.selectedItemPosition){d,which->
   source.setSelection(which);d.dismiss();info.text="Fuente: "+names[which]
  }.setNegativeButton("Cancelar",null).show()
 }
 private fun showMyPack(){
  val dir=File(filesDir,"wa_stickers")
  val count=dir.listFiles()?.count{it.extension=="webp"}?:0
  AlertDialog.Builder(this).setTitle("Mis stickers").setMessage("Tenés $count stickers guardados. WhatsApp necesita al menos 3 para instalar un paquete.")
   .setPositiveButton("Agregar a WhatsApp"){_,_->installPack()}.setNegativeButton("Cerrar",null).show()
 }
 private fun showSettings(){
  val options=arrayOf("Configurar IA: Gemini / Groq","Buscar packs de Telegram","Configurar token de Telegram","Configurar GIPHY y Tenor","Agregar paquete a WhatsApp")
  AlertDialog.Builder(this).setTitle("Ajustes y herramientas").setItems(options){_,which->
   when(which){
    0->configAI()
    1->{val q=query.text.toString().trim();if(q.isBlank())info.text="Escribí una búsqueda primero" else{list.removeAllViews();discoverTelegram(q)}}
    2->configToken()
    3->configGifKeys()
    4->installPack()
   }
  }.setNegativeButton("Cerrar",null).show()
 }
 private fun configAI(){
  val prefs=getPreferences(0)
  val panel=LinearLayout(this).apply{orientation=1;setPadding(28,8,28,8)}
  val enabled=CheckBox(this).apply{text="Usar IA para mejorar las búsquedas";isChecked=prefs.getBoolean("ai_enabled",false)}
  val gemini=EditText(this).apply{hint="Clave API Gemini";setSingleLine(true);setText(prefs.getString("gemini_key",""))}
  val groq=EditText(this).apply{hint="Clave API Groq (respaldo)";setSingleLine(true);setText(prefs.getString("groq_key",""))}
  panel.addView(enabled);panel.addView(gemini);panel.addView(groq)
  AlertDialog.Builder(this).setTitle("Búsqueda inteligente ✦")
   .setMessage("Gemini mejora los términos de búsqueda. Si falla, prueba Groq. Si ambas fallan, usa la búsqueda normal. No busca imágenes directamente en toda la web. Para pruebas personales: nunca distribuyas una APK comercial con claves compartidas.")
   .setView(panel).setPositiveButton("Guardar"){_,_->
    prefs.edit().putBoolean("ai_enabled",enabled.isChecked).putString("gemini_key",gemini.text.toString().trim()).putString("groq_key",groq.text.toString().trim()).apply()
    info.text=if(enabled.isChecked)"IA activada: Gemini → Groq → búsqueda normal" else "IA desactivada"
   }.setNegativeButton("Cancelar",null).show()
 }
 private fun activeModel(url:String,key:String,gemini:Boolean):String {
  val conn=URL(url).openConnection() as HttpURLConnection
  conn.connectTimeout=12000;conn.readTimeout=12000
  if(!gemini)conn.setRequestProperty("Authorization","Bearer "+key)
  try{
   if(conn.responseCode !in 200..299)throw Exception("Modelos HTTP "+conn.responseCode)
   val root=JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
   val arr=root.optJSONArray(if(gemini)"models" else "data")?:return ""
   val names=(0 until arr.length()).map{arr.getJSONObject(it)}.filter{
    if(!gemini)true else {
     val methods=it.optJSONArray("supportedGenerationMethods")
     methods!=null&&(0 until methods.length()).any{i->methods.optString(i)=="generateContent"}
    }
   }.map{it.optString(if(gemini)"name" else "id").removePrefix("models/")}
   val preferred=if(gemini)listOf("gemini-2.5-flash","gemini-2.0-flash","gemini-2.5-flash-lite") else listOf("llama-3.3-70b-versatile","llama-3.1-8b-instant")
   return preferred.firstOrNull{it in names} ?: names.firstOrNull{if(gemini)it.contains("flash") else it.contains("llama")&&!it.contains("guard")}.orEmpty()
  }finally{conn.disconnect()}
 }
 private fun aiQuery(q:String):Pair<String,String>{
  val p=getPreferences(0)
  if(!p.getBoolean("ai_enabled",false))return Pair(q,"")
  val instruction="You optimize sticker image searches. Translate the user request into 2 to 5 short English image search keywords, retaining the subject and mood. Output ONLY the keywords, no punctuation or explanation. User request: "+q.take(180)
  val g=p.getString("gemini_key","").orEmpty()
  val failures=mutableListOf<String>()
  if(g.isNotBlank())try{
   val payload=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("parts",org.json.JSONArray().put(JSONObject().put("text",instruction)))))
   val model=activeModel("https://generativelanguage.googleapis.com/v1beta/models?key="+enc(g),g,true)
   if(model.isBlank())throw Exception("No hay modelos Gemini disponibles")
   val result=JSONObject(postJson("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent",payload.toString(),mapOf("x-goog-api-key" to g)))
   val text=result.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
   if(text.isNotBlank())return Pair(text.take(100),"Gemini")
  }catch(e:Exception){failures.add("Gemini: "+(e.message?:"Error desconocido").take(110))}
  val groq=p.getString("groq_key","").orEmpty()
  if(groq.isNotBlank())try{
   val model=activeModel("https://api.groq.com/openai/v1/models",groq,false)
   if(model.isBlank())throw Exception("No hay modelos Groq disponibles")
   val payload=JSONObject().put("model",model).put("temperature",0.2).put("max_tokens",48)
    .put("messages",org.json.JSONArray().put(JSONObject().put("role","user").put("content",instruction)))
   val result=JSONObject(postJson("https://api.groq.com/openai/v1/chat/completions",payload.toString(),mapOf("Authorization" to "Bearer "+groq)))
   val text=result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
   if(text.isNotBlank())return Pair(text.take(100),"Groq")
  }catch(e:Exception){failures.add("Groq: "+(e.message?:"Error desconocido").take(110))}
  return Pair(q,if(failures.isEmpty())"IA sin claves; búsqueda normal" else failures.joinToString(" | ")+" · búsqueda normal")
 }
 private fun postJson(url:String,body:String,headers:Map<String,String>):String{
  val c=URL(url).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.connectTimeout=12000;c.readTimeout=16000;c.doOutput=true
  c.setRequestProperty("Content-Type","application/json")
  headers.forEach{(k,v)->c.setRequestProperty(k,v)}
  try{
   c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
   if(c.responseCode !in 200..299){
    val status=c.responseCode
    val detail=try{JSONObject(c.errorStream?.bufferedReader()?.use{it.readText()}.orEmpty()).optJSONObject("error")?.optString("message").orEmpty()}catch(_:Exception){""}
    throw Exception("HTTP $status"+if(detail.isNotBlank())": "+detail.take(95) else "")
   }
   return c.inputStream.bufferedReader().use{it.readText()}
  }finally{c.disconnect()}
 }
 private fun configGifKeys(){
  val container=LinearLayout(this).apply{orientation=1;setPadding(28,4,28,4)}
  val g=EditText(this).apply{hint="Clave API GIPHY";setSingleLine(true);setText(getPreferences(0).getString("giphy",""))}
  val t=EditText(this).apply{hint="Clave API Tenor";setSingleLine(true);setText(getPreferences(0).getString("tenor",""))}
  container.addView(g);container.addView(t)
  AlertDialog.Builder(this).setTitle("APIs de stickers y GIFs").setMessage("Las claves se guardan en este teléfono. No se publican en GitHub. Los GIFs se importan como stickers estáticos (primer fotograma).").setView(container)
   .setPositiveButton("Guardar"){_,_->getPreferences(0).edit().putString("giphy",g.text.toString().trim()).putString("tenor",t.text.toString().trim()).apply();info.text="Claves guardadas"}
   .setNegativeButton("Cancelar",null).show()
 }
 private fun configToken(){
  val field=EditText(this).apply{hint="Token de @BotFather";setSingleLine(true)}
  AlertDialog.Builder(this).setTitle("Telegram Bot API").setMessage("El token se guarda solo en este teléfono. No lo compartas ni lo subas a GitHub.").setView(field).setPositiveButton("Guardar"){_,_->getPreferences(0).edit().putString("token",field.text.toString().trim()).apply();info.text="Token guardado localmente"}.setNegativeButton("Cancelar",null).show()
 }
 private fun stickerKeywords(raw:String):String {
  var s=raw.lowercase(java.util.Locale.ROOT).trim()
  val words=mapOf("perros" to "dog","perro" to "dog","gatos" to "cat","gato" to "cat","gatitos" to "kitten","cachorros" to "puppy","cachorro" to "puppy","memes" to "meme","amor" to "love","risa" to "laughing","enojado" to "angry","feliz" to "happy","triste" to "sad","cumpleaños" to "birthday","buenos dias" to "good morning","buenas noches" to "good night")
  for((from,to) in words) s=s.replace(Regex("(?<![\\p{L}])"+Regex.escape(from)+"(?![\\p{L}])"),to)
  s=s.replace(Regex("(?i)\\b(stickers?|pegatinas?)\\b"),"").trim()
  return s.ifBlank{raw}
 }
 private fun searchSearx(q:String,results:MutableList<Item>,errors:MutableList<String>){
  val hosts=listOf("https://searx.be","https://searx.tiekoetter.com","https://search.ononoki.org")
  for(host in hosts){
   try{
    val json=JSONObject(fetchLarge(host+"/search?q="+enc(q)+"&categories=images&format=json&safesearch=1",2_500_000))
    val a=json.optJSONArray("results")?:continue
    var count=0
    for(i in 0 until a.length()){
     val x=a.optJSONObject(i)?:continue
     val original=x.optString("img_src").ifBlank{x.optString("url")}
     val thumb=x.optString("thumbnail_src").ifBlank{x.optString("thumbnail").ifBlank{original}}
     if(!original.startsWith("https://")||!thumb.startsWith("https://"))continue
     results.add(Item(x.optString("title",q),thumb,original,"SearXNG · "+x.optString("engine","imágenes")))
     count++
     if(count>=45)break
    }
    if(count>0)return
   }catch(e:Exception){errors.add("SearXNG "+host.substringAfter("https://")+": "+(e.message?:"Error").take(45))}
  }
 }
 private fun search(){
  val q=query.text.toString().trim();if(q.isBlank()){info.text="Escribí algo para buscar";return}
  list.removeAllViews();info.text="Buscando…"
  val mode=source.selectedItemPosition
  if(q.contains("t.me/addstickers/")){telegram(q);return}
  if(mode==4){discoverTelegram(q);return}
  Thread{
   val results=mutableListOf<Item>();val errors=mutableListOf<String>()
   val (optimized,engine)=if(mode==0)Pair(q,"") else aiQuery(q)
   if(engine.isNotBlank())ui.post{info.text=if(engine=="Gemini"||engine=="Groq")"✦ $engine: buscando «$optimized»…" else engine}
   val searchTerm=stickerKeywords(optimized)
   // Public SearXNG instances currently reject automated JSON image requests; disabled by default.
   val stickerTerm=searchTerm+" cartoon sticker illustration"
   if(mode==9){searchWebImages(searchTerm,"Google",results,errors)}
   if(mode==10){searchWebImages(searchTerm,"Bing",results,errors)}
   if(mode==7){searchIndexedImages(searchTerm,"pinterest.com","Pinterest",results,errors)}
   if(mode==8){searchIndexedImages(searchTerm,"instagram.com","Instagram",results,errors)}
   if(mode==5){searchGiphy(searchTerm,results,errors)}
   if(mode==6){searchTenor(searchTerm,results,errors)}
   if(mode==0||mode==1)try{
    val u="https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="+enc(stickerTerm)+"&gsrnamespace=6&gsrlimit=40&prop=imageinfo&iiprop=url&iiurlwidth=320&format=json"
    val pages=JSONObject(fetch(u)).optJSONObject("query")?.optJSONObject("pages")
    if(pages!=null){val it=pages.keys();while(it.hasNext()){val page=pages.getJSONObject(it.next());val im=page.optJSONArray("imageinfo")?.optJSONObject(0)?:continue;val original=im.optString("url");if(original.startsWith("https://")&&original.matches(Regex("(?i).*\\.(png|jpe?g|webp)(\\?.*)?$")))results.add(Item(page.optString("title").removePrefix("File:"),im.optString("thumburl",original),original,"Wikimedia"))}}
   }catch(e:Exception){errors.add("Wikimedia: ${e.message}")}
   if(mode==2)try{
    val j=JSONObject(fetch("https://api.openverse.org/v1/images/?q="+enc(stickerTerm)+"&page_size=40"))
    val a=j.optJSONArray("results")
    if(a!=null)for(i in 0 until a.length()){val x=a.getJSONObject(i);val u=x.optString("url");if(u.startsWith("https://"))results.add(Item(x.optString("title","Imagen"),x.optString("thumbnail",u),u,"Openverse · ${x.optString("license")}"))}
   }catch(e:Exception){errors.add("Openverse: ${e.message}")}
   if(mode==0||mode==3)try{
    val catalog=org.json.JSONArray(fetchLarge("https://raw.githubusercontent.com/hfg-gmuend/openmoji/master/data/openmoji.json",5_000_000))
    var added=0
    for(i in 0 until catalog.length()){
     val e=catalog.getJSONObject(i)
     val tags=e.optString("annotation")+" "+e.optString("tags")+" "+e.optString("group")+" "+e.optString("subgroups")
     if(tags.contains(searchTerm,true)||tags.contains(q,true)){
      val hex=e.optString("hexcode")
      if(hex.matches(Regex("[A-F0-9-]+"))){
       val url="https://cdn.jsdelivr.net/gh/hfg-gmuend/openmoji@master/color/618x618/"+hex+".png"
       results.add(Item(e.optString("annotation","Emoji"),url,url,"OpenMoji · CC BY-SA 4.0"))
       added++;if(added>=30)break
      }
     }
    }
   }catch(e:Exception){errors.add("OpenMoji: ${e.message}")}
   val seen=HashSet<String>()
   val unique=results.filter{seen.add(it.url)}
   ui.post{info.text=(if(engine=="Gemini"||engine=="Groq")"✦ $engine · " else if(engine.isNotBlank())engine+" · " else "")+"${unique.size} resultados"+if(errors.isNotEmpty())" · "+errors.joinToString("; ") else "";unique.forEach{add(it)}}
  }.start()
 }
 private fun searchWebImages(q:String,engine:String,results:MutableList<Item>,errors:MutableList<String>){
  try{
   val target=if(engine=="Google")"https://www.google.com/search?tbm=isch&hl=es&q="+enc(q+" sticker png") else "https://www.bing.com/images/search?q="+enc(q+" sticker png")+"&first=1"
   val html=fetchLarge(target,3_000_000)
   val links=LinkedHashSet<String>()
   val decoded=html.replace("&quot;","\"").replace("&amp;","&").replace("&#39;","'")
    .replace("\\u003d","=").replace("\\u0026","&").replace("\\/","/")
   if(engine=="Bing"){
    Regex("""murl\s*[:=]\s*["'](https?[^"']+)""",RegexOption.IGNORE_CASE).findAll(decoded).forEach{links.add(it.groupValues[1])}
   }
   Regex("""https?[^"<>\s\\]+?\.(?:jpg|jpeg|png|webp)(?:\?[^"<>\s\\]*)?""",RegexOption.IGNORE_CASE)
    .findAll(decoded).forEach{links.add(it.value)}
   var added=0
   for(raw in links){
    val url=raw.trim().replace("&amp;","&")
    if(!url.startsWith("https://")||url.length>1600||url.contains("google.com/images/branding")||url.contains("bing.com/rp/")||url.contains("gstatic.com"))continue
    results.add(Item(q,url,url,engine+" Imágenes"))
    added++
    if(added>=24)break
   }
   if(added==0)errors.add("$engine: no se pudieron obtener imágenes (el buscador puede bloquear el acceso)")
  }catch(e:Exception){errors.add("$engine: "+(e.message?:"Error"))}
 }
 private fun searchIndexedImages(q:String,domain:String,label:String,results:MutableList<Item>,errors:MutableList<String>){
  try{
   // Public search index only; no authentication, cookies, or private content.
   val html=fetchLarge("https://www.bing.com/images/search?q="+enc("site:"+domain+" "+q)+"&form=HDRSC3",2_000_000)
   val rx=Regex("""(?:murl|imgurl)[^\\n]{0,30}?(https?[^"\\\\ <]+)""",RegexOption.IGNORE_CASE)
   val candidates=rx.findAll(html).map{it.groupValues[1].replace("&amp;","&").replace("\\u0026","&")}.distinct().take(24).toList()
   for(url in candidates){
    if(url.startsWith("https://")&&url.length<1800)results.add(Item(q,url,url,label+" · índice público"))
   }
   if(candidates.isEmpty())errors.add("$label: índice sin resultados")
  }catch(e:Exception){errors.add("$label: "+(e.message?:"Error de búsqueda"))}
 }
 private fun searchGiphy(q:String,results:MutableList<Item>,errors:MutableList<String>){
  val key=getPreferences(0).getString("giphy","").orEmpty()
  if(key.isBlank()){errors.add("GIPHY requiere clave API");return}
  try{
   val j=JSONObject(fetch("https://api.giphy.com/v1/stickers/search?api_key="+enc(key)+"&q="+enc(q)+"&limit=24&rating=g"))
   val arr=j.optJSONArray("data")?:return
   for(i in 0 until arr.length()){
    val x=arr.getJSONObject(i);val images=x.optJSONObject("images")?:continue
    val fixed=images.optJSONObject("fixed_width_still")?.optString("url").orEmpty()
    val original=images.optJSONObject("original_still")?.optString("url").orEmpty()
    val url=if(original.startsWith("https://"))original else fixed
    if(url.startsWith("https://"))results.add(Item(x.optString("title","Sticker"),fixed.ifBlank{url},url,"GIPHY · imagen estática"))
   }
  }catch(e:Exception){errors.add("GIPHY: ${e.message}")}
 }
 private fun searchTenor(q:String,results:MutableList<Item>,errors:MutableList<String>){
  val key=getPreferences(0).getString("tenor","").orEmpty()
  if(key.isBlank()){errors.add("Tenor requiere clave API");return}
  try{
   val j=JSONObject(fetch("https://tenor.googleapis.com/v2/search?key="+enc(key)+"&q="+enc(q)+"&limit=24&media_filter=gif,tinygif"))
   val arr=j.optJSONArray("results")?:return
   for(i in 0 until arr.length()){
    val x=arr.getJSONObject(i);val formats=x.optJSONObject("media_formats")?:continue
    val gif=formats.optJSONObject("tinygif")?:formats.optJSONObject("gif")?:continue
    val preview=gif.optString("preview").ifBlank{gif.optString("url")}
    val url=gif.optString("url")
    if(preview.startsWith("https://"))results.add(Item(x.optString("content_description","GIF"),preview,preview,"Tenor · fotograma"))
   }
  }catch(e:Exception){errors.add("Tenor: ${e.message}")}
 }
 private fun discoverTelegram(term:String){
  info.text="Buscando paquetes públicos de Telegram para: $term…"
  Thread{
   try{
    val searchUrl="https://www.bing.com/search?format=rss&q="+enc("site:t.me/addstickers/ "+term+" sticker pack")
    val xml=fetchLarge(searchUrl,500_000)
    val rx=Regex("(?i)(?:https?://)?(?:t\\.me|telegram\\.me)/addstickers/([A-Za-z0-9_]+)")
    val found=rx.findAll(xml.replace("&amp;","&")).map{it.groupValues[1]}.distinct().take(20).toList()
    ui.post{
     list.removeAllViews()
     if(found.isEmpty()){
      info.text="No encontré packs públicos para '$term'. Probá otro término o pegá un enlace t.me/addstickers/..."
     }else{
      info.text="${found.size} paquetes encontrados en la web. Elegí uno para importar."
      found.forEach{name->
       val btn=TextView(this).apply{text="📦 "+name.replace("_"," ");textSize=16f;setTextColor(Color.WHITE);setPadding(20,20,20,20);setOnClickListener{list.removeAllViews();telegram("https://t.me/addstickers/"+name)}}
       list.addView(btn)
      }
     }
    }
   }catch(e:Exception){ui.post{info.text="No se pudo buscar packs públicos: ${e.message}. Pegá un enlace de Telegram."}}
  }.start()
 }
 private fun telegram(q:String){
  val token=getPreferences(0).getString("token","")?:""
  if(token.isBlank()){info.text="Configurá primero tu token de bot Telegram (@BotFather).";return}
  val name=q.substringAfter("addstickers/",q).substringBefore("?").substringBefore("/").trim()
  if(!name.matches(Regex("[A-Za-z0-9_]{1,100}"))){
   info.text="Telegram no busca por palabra. Pegá el enlace completo t.me/addstickers/NombreDelPack"
   return
  }
  Thread{try{
   ui.post{info.text="Conectando con Telegram…"}
   val base="https://api.telegram.org/bot"+token+"/"
   val response=JSONObject(fetch(base+"getStickerSet?name="+enc(name)))
   if(!response.optBoolean("ok"))throw Exception("Telegram: "+response.optString("description"))
   val pack=response.getJSONObject("result");val stickers=pack.getJSONArray("stickers");val items=mutableListOf<Item>();var skipped=0
   for(i in 0 until minOf(stickers.length(),30)){
    val s=stickers.getJSONObject(i)
    if(s.optBoolean("is_animated")||s.optBoolean("is_video")){skipped++;continue}
    val file=JSONObject(fetch(base+"getFile?file_id="+enc(s.getString("file_id")))).getJSONObject("result").getString("file_path")
    val url="https://api.telegram.org/file/bot"+token+"/"+file
    items.add(Item(s.optString("emoji","Sticker")+" #"+(i+1),url,url,"Telegram"))
   }
   ui.post{
    info.text=pack.optString("title",name)+": ${items.size} stickers estáticos · ${skipped} animados omitidos"
    if(items.isNotEmpty()){
     list.addView(Button(this).apply{text="⬇ Importar stickers de Telegram al paquete";setOnClickListener{importTelegram(items)}})
    }
    items.forEach{add(it)}
   }
  }catch(e:Exception){ui.post{info.text="Error de importación: ${e.message}"}}}.start()
 }
 private fun importTelegram(items:List<Item>){
  info.text="Importando stickers de Telegram…"
  Thread{
   var ok=0;var fail=0
   for(item in items){
    try{storeSticker(bytes(item.url,10_000_000),false);ok++}catch(_:Exception){fail++}
    val done=ok+fail
    ui.post{info.text="Telegram: $done/${items.size} · guardados $ok · errores $fail"}
   }
   ui.post{info.text="Importación terminada: $ok stickers guardados, $fail omitidos. Tocá Agregar paquete a WhatsApp."}
  }.start()
 }
 private fun add(item:Item){
  val density=resources.displayMetrics.density
  val dp={n:Int->(n*density).toInt()}
  val width=resources.displayMetrics.widthPixels-dp(36)
  val gap=dp(6)
  val tile=(width-gap*6)/3
  val box=LinearLayout(this).apply{
   orientation=1
   setPadding(dp(4),dp(4),dp(4),dp(4))
   background=android.graphics.drawable.GradientDrawable().apply{
    setColor(Color.rgb(29,32,52));cornerRadius=dp(12).toFloat()
   }
  }
  val img=ImageView(this).apply{
   scaleType=ImageView.ScaleType.FIT_CENTER
   setBackgroundColor(Color.rgb(38,41,61))
   setOnClickListener{preview(item)}
  }
  box.addView(img,LinearLayout.LayoutParams(-1,tile-dp(12)))
  box.addView(TextView(this).apply{
   text="＋ Agregar";gravity=Gravity.CENTER;textSize=11f;setTypeface(null,1)
   setTextColor(Color.WHITE)
   background=android.graphics.drawable.GradientDrawable().apply{
    setColor(Color.rgb(124,82,245));cornerRadius=dp(8).toFloat()
   }
   setOnClickListener{convert(item.url)}
  },LinearLayout.LayoutParams(-1,dp(32)).apply{topMargin=dp(4)})
  val params=GridLayout.LayoutParams().apply{
   this.width=tile;this.height=tile+dp(32)
   setMargins(gap,gap,gap,gap)
  }
  list.addView(box,params)
  imagePool.execute {
   try{
    val data=bytes(item.thumb,5_000_000)
    val bmp=BitmapFactory.decodeByteArray(data,0,data.size)?:throw Exception("No es una imagen")
    ui.post{img.setImageBitmap(bmp)}
   }catch(e:Exception){
    ui.post{
     img.setBackgroundColor(Color.rgb(47,48,65))
     img.setImageDrawable(null)
     val fallback=android.graphics.drawable.GradientDrawable().apply{setColor(Color.rgb(47,48,65));cornerRadius=dp(8).toFloat()}
     img.background=fallback
     img.contentDescription="Imagen no disponible: "+(e.message?:"Error")
     img.setOnClickListener{Toast.makeText(this,"Imagen no disponible",Toast.LENGTH_SHORT).show()}
    }
   }
  }
 }
 private fun preview(item:Item){
  val viewer=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;setPadding(12,12,12,12)}
  val dialog=AlertDialog.Builder(this).setTitle(item.title).setView(viewer)
   .setPositiveButton("Agregar al paquete"){_,_->convert(item.url)}
   .setNegativeButton("Cerrar",null).create()
  dialog.show()
  Thread{try{val data=bytes(item.thumb,5_000_000);val bmp=BitmapFactory.decodeByteArray(data,0,data.size);ui.post{viewer.setImageBitmap(bmp)}}catch(_:Exception){}}.start()
 }
 private fun convert(url:String){info.text="Convirtiendo a sticker…";Thread{try{storeSticker(bytes(url,10_000_000))}catch(e:Exception){ui.post{info.text="Error: ${e.message}"}}}.start()}
 private fun storeSticker(data:ByteArray,autoInstall:Boolean=true){
  val original=BitmapFactory.decodeByteArray(data,0,data.size)?:throw Exception("Formato no compatible")
  val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
  val canvas=Canvas(bitmap);canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR)
  val scale=minOf(512f/original.width,512f/original.height)
  val w=original.width*scale;val h=original.height*scale
  canvas.drawBitmap(original,null,RectF((512-w)/2,(512-h)/2,(512+w)/2,(512+h)/2),Paint(3))
  val dir=File(filesDir,"wa_stickers").apply{mkdirs()}
  val count=dir.listFiles()?.count{it.extension=="webp"}?:0
  if(count>=30)throw Exception("El paquete ya tiene 30 stickers")
  val file=File(dir,"sticker_"+System.currentTimeMillis()+"_"+(0..999).random()+".webp")
  val output=ByteArrayOutputStream()
  var quality=85
  do{
   output.reset()
   bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY,quality,output)
   quality-=10
  }while(output.size()>100_000&&quality>=25)
  if(output.size()>100_000)throw Exception("Sticker supera 100 KB")
  file.writeBytes(output.toByteArray())
  if(!File(dir,"tray.png").exists()){
   val icon=Bitmap.createScaledBitmap(bitmap,96,96,true)
   FileOutputStream(File(dir,"tray.png")).use{icon.compress(Bitmap.CompressFormat.PNG,100,it)}
  }
  ui.post{
   info.text="Sticker añadido al paquete. Necesitás al menos 3 para instalarlo en WhatsApp."
   if(autoInstall&&(dir.listFiles()?.count{it.extension=="webp"}?:0)>=3)installPack()
  }
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==17&&resultCode==RESULT_OK){val uri=data?.data?:return;Thread{try{storeSticker(contentResolver.openInputStream(uri)!!.use{it.readBytes()})}catch(e:Exception){ui.post{info.text=e.message}}}.start()}}
 private fun installPack(){
  val dir=File(filesDir,"wa_stickers")
  val count=dir.listFiles()?.count{it.extension=="webp"}?:0
  if(count<3){info.text="Agregá al menos 3 stickers (tenés $count).";return}
  try{
   val intent=Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply{
    putExtra("sticker_pack_id",StickerProvider.PACK)
    putExtra("sticker_pack_authority",StickerProvider.AUTH)
    putExtra("sticker_pack_name","Mis stickers StickerIA")
    setPackage("com.whatsapp")
   }
   startActivityForResult(intent,21)
  }catch(e:Exception){info.text="WhatsApp no pudo abrir el paquete: ${e.message}"}
 }
 private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
 private fun fetch(url:String):String=String(bytes(url,3_000_000),Charsets.UTF_8)
 private fun fetchLarge(url:String,max:Int):String=String(bytes(url,max),Charsets.UTF_8)
 private fun bytes(url:String,limit:Int):ByteArray{
  val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=12000;c.readTimeout=18000;c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");c.setRequestProperty("Accept","image/avif,image/webp,image/png,image/jpeg,image/*;q=0.8,*/*;q=0.5")
  try{if(c.responseCode !in 200..299)throw Exception("HTTP ${c.responseCode}");val out=ByteArrayOutputStream();c.inputStream.use{input->val buf=ByteArray(8192);while(true){val n=input.read(buf);if(n<0)break;if(out.size()+n>limit)throw Exception("Archivo demasiado grande");out.write(buf,0,n)}};return out.toByteArray()}finally{c.disconnect()}
 }
}
