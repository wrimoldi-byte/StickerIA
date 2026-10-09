package com.stickeria.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 56, 32, 24)
            setBackgroundColor(Color.rgb(248, 249, 253))
        }
        val title = TextView(this).apply {
            text = "✨ StickerIA"
            textSize = 32f
            setTextColor(Color.rgb(40, 44, 65))
        }
        val subtitle = TextView(this).apply {
            text = "Buscador de stickers para WhatsApp — versión de prueba"
            textSize = 17f
            setPadding(0, 12, 0, 30)
        }
        val search = EditText(this).apply {
            hint = "Ej: gatos graciosos"
            setSingleLine(true)
        }
        val message = TextView(this).apply {
            text = "Esta primera APK verifica la instalación Android. La búsqueda automática en Telegram, conversión y generación IA necesitan sus respectivas integraciones y aún no están disponibles."
            textSize = 15f
            setPadding(0, 20, 0, 16)
        }
        val searchButton = Button(this).apply {
            text = "Buscar en la web"
            setOnClickListener {
                val q = search.text.toString().trim()
                if (q.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Escribí qué sticker buscás", Toast.LENGTH_SHORT).show()
                } else {
                    val url = "https://www.google.com/search?tbm=isch&q=" + Uri.encode(q + " stickers")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
        val telegramButton = Button(this).apply {
            text = "Abrir paquete público de Telegram"
            setOnClickListener {
                val raw = search.text.toString().trim()
                val name = if (raw.startsWith("https://t.me/addstickers/")) raw.substringAfterLast("/").substringBefore("?") else raw
                if (name.matches(Regex("[A-Za-z0-9_]{1,64}"))) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/addstickers/" + name)))
                } else {
                    Toast.makeText(this@MainActivity, "Pegá un enlace t.me/addstickers/Nombre", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(search)
        root.addView(searchButton)
        root.addView(telegramButton)
        root.addView(message)
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
