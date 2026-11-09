package com.aokiyoshi.minamo

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File

/**
 * Lets Minamo's web page save its backup file on Android.
 * The page calls it as: MinamoApp.saveBackup('minamo-backup.json', text)
 */
class MinamoBridge(private val ctx: Context) {

    @JavascriptInterface
    fun saveBackup(name: String, text: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: saves into the public Downloads folder, no permission needed
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    ?: return false
                toast("Backup saved to Downloads")
            } else {
                // Android 9 and older: save in the app's own Downloads folder (no permission needed)
                val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
                dir.mkdirs()
                File(dir, name).writeText(text)
                toast("Backup saved in the app's Downloads folder")
            }
            true
        } catch (e: Exception) {
            toast("Could not save the backup")
            false
        }
    }

    private fun toast(msg: String) {
        // JavascriptInterface methods run on a background thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
