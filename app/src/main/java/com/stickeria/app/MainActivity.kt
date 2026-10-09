package com.stickeria.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.net.*
import java.io.*

class MainActivity : Activity() {
 private val ui=Handler(Looper.getMainLooper())
 private lateinit var list:LinearLayout
 private lateinit var info:TextView
 private lateinit var query:EditText
 private lateinit var source:Spinner
 private val names=arrayOf("Todas (Wikimedia + Openverse + OpenMoji)","Wikimedia Commons","Openverse","OpenMoji (emojis)","Telegram (enlace de pack)")
 private data class Item(val title:String,val thumb:String,val url:String,val origin:String)
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=Color.rgb(20,27,48)
  window.navigationBarColor=Color.rgb(20,27,48)
  val root=LinearLayout(this).apply{orientation=1;setPadding(16,12,16,8);setBackgroundColor(Color.rgb(245,247,251))}
  root.addView(TextView(this).apply{text="StickerIA";textSize=27f;setTypeface(null,1);setTextColor(Color.rgb(24,34,62));setPadding(4,4,4,0)})
  root.addView(TextView(this).apply{text="Encontrá stickers y agregalos a WhatsApp";textSize=13f;setTextColor(Color.rgb(94,106,128));setPadding(4,0,4,10)})
  query=EditText(this).apply{hint="Buscar stickers o pegar enlace de Telegram";setSingleLine(true);textSize=16f;setPadding(16,4,16,4);setBackgroundColor(Color.WHITE)}
  root.addView(query,LinearLayout.LayoutParams(-1,52))
  source=Spinner(this);source.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  root.addView(source,LinearLayout.LayoutParams(-1,46))
  val actions=LinearLayout(this).apply{orientation=0}
  actions.addView(Button(this).apply{text="Buscar";setAllCaps=false;setOnClickListener{search()}},LinearLayout.LayoutParams(0,52,1f))
  actions.addView(Button(this).apply{text="Mi galería";setAllCaps=false;setOnClickListener{startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*"},17)}},LinearLayout.LayoutParams(0,52,1f))
  root.addView(actions)
  root.addView(Button(this).apply{text="Agregar mi paquete a WhatsApp";setAllCaps=false;setTextColor(Color.WHITE);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(21,151,102)));setOnClickListener{installPack()}},LinearLayout.LayoutParams(-1,52))
  root.addView(TextView(this).apply{text="Telegram";textSize=13f;setTextColor(Color.rgb(94,106,128));setPadding(4,8,0,0)})
  root.addView(Button(this).apply{text="Configurar acceso a Telegram";setAllCaps=false;setOnClickListener{configToken()}},LinearLayout.LayoutParams(-1,46))
  info=TextView(this).apply{text="Elegí una fuente y buscá.";textSize=13f;setTextColor(Color.rgb(64,76,100));setPadding(4,8,4,8)}
  root.addView(info)
  val scroll=ScrollView(this);list=LinearLayout(this).apply{orientation=1};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root)
 }
 private fun configToken(){
  val field=EditText(this).apply{hint="Token de @BotFather";setSingleLine(true)}
  AlertDialog.Builder(this).setTitle("Telegram Bot API").setMessage("El token se guarda solo en este teléfono. No lo compartas ni lo subas a GitHub.").setView(field).setPositiveButton("Guardar"){_,_->getPreferences(0).edit().putString("token",field.text.toString().trim()).apply();info.text="Token guardado localmente"}.setNegativeButton("Cancelar",null).show()
 }
 private fun search(){
  val q=query.text.toString().trim();if(q.isBlank()){info.text="Escribí algo para buscar";return}
  list.removeAllViews();info.text="Buscando…"
  val mode=source.selectedItemPosition
  if(mode==4 || q.contains("t.me/addstickers/")){telegram(q);return}
  Thread{
   val results=mutableListOf<Item>();val errors=mutableListOf<String>()
   if(mode==0||mode==1)try{
    val u="https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="+enc(q)+"&gsrnamespace=6&gsrlimit=20&prop=imageinfo&iiprop=url&iiurlwidth=320&format=json"
    val pages=JSONObject(fetch(u)).optJSONObject("query")?.optJSONObject("pages")
    if(pages!=null){val it=pages.keys();while(it.hasNext()){val page=pages.getJSONObject(it.next());val im=page.optJSONArray("imageinfo")?.optJSONObject(0)?:continue;val original=im.optString("url");if(original.startsWith("https://")&&original.matches(Regex("(?i).*\\.(png|jpe?g|webp)(\\?.*)?$")))results.add(Item(page.optString("title").removePrefix("File:"),im.optString("thumburl",original),original,"Wikimedia"))}}
   }catch(e:Exception){errors.add("Wikimedia: ${e.message}")}
   if(mode==0||mode==2)try{
    val j=JSONObject(fetch("https://api.openverse.org/v1/images/?q="+enc(q)+"&page_size=20"))
    val a=j.optJSONArray("results")
    if(a!=null)for(i in 0 until a.length()){val x=a.getJSONObject(i);val u=x.optString("url");if(u.startsWith("https://"))results.add(Item(x.optString("title","Imagen"),x.optString("thumbnail",u),u,"Openverse · ${x.optString("license")}"))}
   }catch(e:Exception){errors.add("Openverse: ${e.message}")}
   if(mode==0||mode==3)try{
    val catalog=org.json.JSONArray(fetchLarge("https://raw.githubusercontent.com/hfg-gmuend/openmoji/master/data/openmoji.json",5_000_000))
    var added=0
    for(i in 0 until catalog.length()){
     val e=catalog.getJSONObject(i)
     val tags=e.optString("annotation")+" "+e.optString("tags")+" "+e.optString("group")+" "+e.optString("subgroups")
     if(tags.contains(q,true)){
      val hex=e.optString("hexcode")
      if(hex.matches(Regex("[A-F0-9-]+"))){
       val url="https://cdn.jsdelivr.net/gh/hfg-gmuend/openmoji@master/color/618x618/"+hex+".png"
       results.add(Item(e.optString("annotation","Emoji"),url,url,"OpenMoji · CC BY-SA 4.0"))
       added++;if(added>=30)break
      }
     }
    }
   }catch(e:Exception){errors.add("OpenMoji: ${e.message}")}
   ui.post{info.text="${results.size} resultados"+if(errors.isNotEmpty())" · "+errors.joinToString("; ") else "";results.forEach{add(it)}}
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
    try{storeSticker(bytes(item.url,10_000_000));ok++}catch(_:Exception){fail++}
    val done=ok+fail
    ui.post{info.text="Telegram: $done/${items.size} · guardados $ok · errores $fail"}
   }
   ui.post{info.text="Importación terminada: $ok stickers guardados, $fail omitidos. Tocá Agregar paquete a WhatsApp."}
  }.start()
 }
 private fun add(item:Item){
  val box=LinearLayout(this).apply{orientation=1;setPadding(12,12,12,12);setBackgroundColor(Color.WHITE)}
  val img=ImageView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,330);scaleType=ImageView.ScaleType.FIT_CENTER}
  img.setOnClickListener { preview(item) };box.addView(img);box.addView(TextView(this).apply{text=item.title+" · "+item.origin;maxLines=2})
  box.addView(Button(this).apply{text="＋ Agregar sticker";setOnClickListener{convert(item.url)}})
  list.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12})
  Thread{try{val data=bytes(item.thumb,5_000_000);val bmp=BitmapFactory.decodeByteArray(data,0,data.size);ui.post{img.setImageBitmap(bmp)}}catch(_:Exception){}}.start()
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
 private fun storeSticker(data:ByteArray){
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
   if((dir.listFiles()?.count{it.extension=="webp"}?:0)>=3)installPack()
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
  val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=12000;c.readTimeout=18000;c.setRequestProperty("User-Agent","StickerIA/0.3 (Android)")
  try{if(c.responseCode !in 200..299)throw Exception("HTTP ${c.responseCode}");val out=ByteArrayOutputStream();c.inputStream.use{input->val buf=ByteArray(8192);while(true){val n=input.read(buf);if(n<0)break;if(out.size()+n>limit)throw Exception("Archivo demasiado grande");out.write(buf,0,n)}};return out.toByteArray()}finally{c.disconnect()}
 }
}
