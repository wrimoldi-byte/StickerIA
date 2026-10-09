package com.stickeria.app
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

class StickerProvider:ContentProvider(){
 companion object { const val AUTH="com.stickeria.app.stickers"; const val PACK="stickeria_pack" }
 override fun onCreate()=true
 override fun getType(uri:Uri):String?=if(uri.lastPathSegment?.endsWith(".webp")==true)"image/webp" else "vnd.android.cursor.dir/vnd.com.whatsapp.sticker"
 override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?{
  val parts=uri.pathSegments
  if(parts.isEmpty())return null
  val files=context!!.filesDir.resolve("wa_stickers").listFiles()?.filter{it.extension=="webp"}?.sortedBy{it.name}?:emptyList()
  return when(parts[0]){
   "metadata"->MatrixCursor(arrayOf("sticker_pack_identifier","sticker_pack_name","sticker_pack_publisher","sticker_pack_icon","android_play_store_link","ios_app_store_link","publisher_email","publisher_website","privacy_policy_website","license_agreement_website","image_data_version","avoid_cache","animated_sticker_pack")).apply{addRow(arrayOf(PACK,"Mis stickers StickerIA","StickerIA","tray.png","","","","","","","1",0,0))}
   "stickers"->MatrixCursor(arrayOf("sticker_file_name","sticker_emoji")).apply{files.forEach{addRow(arrayOf(it.name,"😀"))}}
   "stickers_asset"->MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE)).apply{if(parts.size>=3){val f=asset(parts.last());if(f.exists())addRow(arrayOf(f.name,f.length()))}}
   else->null
  }
 }
 private fun asset(name:String):File{
  require(name=="tray.png"||name.matches(Regex("sticker_[0-9]+\\.webp")))
  return if(name=="tray.png")File(context!!.filesDir,"wa_stickers/tray.png") else File(context!!.filesDir,"wa_stickers/$name")
 }
 override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor?{
  val name=uri.lastPathSegment?:return null
  val file=asset(name)
  return if(file.exists())ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY) else null
 }
 override fun insert(uri:Uri,values:ContentValues?):Uri?=null
 override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int=0
 override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int=0
}
