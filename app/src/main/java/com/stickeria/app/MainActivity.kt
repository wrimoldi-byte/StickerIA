package com.stickeria.app

import android.app.Activity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import android.net.Uri
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.File

class MainActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var grid: LinearLayout
    private lateinit var status: TextView
    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 249, 253))
            setPadding(22, 35, 22, 12)
        }
        val title = TextView(this).apply {
            text = "✨ StickerIA"
            textSize = 29f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(34, 38, 65))
        }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Buscá imágenes para stickers sin salir de la app"
            textSize = 14f
            setPadding(0, 8, 0, 16)
        })
        search = EditText(this).apply {
            hint = "Gatos, memes, corazones..."
            setSingleLine(true)
        }
        root.addView(search)
        val button = Button(this).apply {
            text = "Buscar stickers"
            setOnClickListener { searchImages(search.text.toString()) }
        }
        root.addView(button)
        root.addView(TextView(this).apply {
            text = "Fuente actual: Wikimedia Commons · Imágenes con licencias identificables"
            textSize = 12f
        })
        status = TextView(this).apply {
            text = "Escribí una búsqueda para empezar."
            setPadding(0, 16, 0, 12)
            textSize = 14f
        }
        root.addView(status)
        val scroll = ScrollView(this)
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun searchImages(query: String) {
        if (query.isBlank()) {
            status.text = "Escribí una palabra primero."
            return
        }
        status.text = "Buscando resultados..."
        grid.removeAllViews()
        Thread {
            try {
                val encoded = URLEncoder.encode(query + " sticker", "UTF-8")
                val api = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=" +
                    encoded + "&gsrnamespace=6&gsrlimit=24&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=320&format=json"
                val data = request(api)
                val pages = JSONObject(data).optJSONObject("query")?.optJSONObject("pages")
                val results = mutableListOf<Triple<String,String,String>>()
                if (pages != null) {
                    val keys = pages.keys()
                    while (keys.hasNext()) {
                        val page = pages.getJSONObject(keys.next())
                        val image = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                        val original = image.optString("url")
                        val thumb = image.optString("thumburl", original)
                        val name = page.optString("title").removePrefix("File:")
                        if (original.startsWith("https://") && thumb.startsWith("https://")) {
                            results.add(Triple(name, thumb, original))
                        }
                    }
                }
                main.post {
                    status.text = if (results.isEmpty()) "No encontramos resultados. Probá con otra palabra." else "${results.size} resultados. Tocá una imagen para verla o guardarla."
                    results.forEach { addResult(it.first, it.second, it.third) }
                }
            } catch (e: Exception) {
                main.post { status.text = "No se pudo completar la búsqueda: ${e.message ?: "sin conexión"}" }
            }
        }.start()
    }

    private fun addResult(name: String, thumbnail: String, original: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 22f
                setStroke(1, Color.rgb(223, 226, 232))
            }
        }
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 230)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = name
        }
        card.addView(imageView)
        card.addView(TextView(this).apply {
            text = name
            maxLines = 2
            textSize = 14f
            setPadding(0, 6, 0, 8)
        })
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Ver fuente"
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://commons.wikimedia.org/wiki/Special:FilePath/" + Uri.encode(name)))) }
        })
        actions.addView(Button(this).apply {
            text = "Guardar"
            setOnClickListener { saveImage(original) }
        })
        card.addView(actions)
        grid.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })
        Thread {
            try {
                val conn = URL(thumbnail).openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.setRequestProperty("User-Agent", "StickerIA-MVP/0.2 (Android; educational prototype)")
                conn.inputStream.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    main.post { imageView.setImageBitmap(bitmap) }
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }.start()
    }

    private fun saveImage(url: String) {
        status.text = "Guardando imagen..."
        Thread {
            try {
                val extension = when {
                    url.contains(".png", true) -> ".png"
                    url.contains(".webp", true) -> ".webp"
                    url.contains(".gif", true) -> ".gif"
                    else -> ".jpg"
                }
                val file = File(filesDir, "sticker_" + System.currentTimeMillis() + extension)
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "StickerIA-MVP/0.2 (Android; educational prototype)")
                if (conn.contentLengthLong > 8_000_000L) throw IllegalArgumentException("Imagen demasiado grande")
                conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                conn.disconnect()
                main.post { status.text = "Imagen guardada dentro de StickerIA. La exportación a WhatsApp llegará en una próxima versión." }
            } catch (e: Exception) {
                main.post { status.text = "No se pudo guardar: ${e.message}" }
            }
        }.start()
    }

    private fun request(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.setRequestProperty("User-Agent", "StickerIA-MVP/0.2 (Android; educational prototype)")
        return try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
    }
}
