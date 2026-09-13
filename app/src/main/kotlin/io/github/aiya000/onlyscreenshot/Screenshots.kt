package io.github.aiya000.onlyscreenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a silently taken screenshot next to the ones the system takes, in
 * Pictures/Screenshots. Scoped storage owns the file, so no storage permission is asked
 * for and nothing is left behind if the app is uninstalled.
 *
 * Returns the file name that was written, or null where it could not be.
 */
fun Context.saveScreenshot(bitmap: Bitmap): String? {
    val name = "Screenshot_${timestamp()}.png"
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/Screenshots",
        )
        // Kept out of the gallery until the bytes are actually there.
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val uri = contentResolver.insert(collection, values) ?: return null
    return try {
        contentResolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("compress failed")
        } ?: throw IOException("no output stream")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        name
    } catch (e: IOException) {
        contentResolver.delete(uri, null, null)
        null
    }
}

private fun timestamp(): String =
    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
