package managers

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StorageManager(private val context: Context) {

    companion object {
        private const val MEDIA_DIRECTORY = "media"
    }

    private val mediaStorageDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), MEDIA_DIRECTORY).apply {
            mkdirs()
        }
    }

    fun saveMediaToStorage(bitmap: Bitmap, filename: String): Uri? {
        val file = File(mediaStorageDir, "$filename.jpg")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val uri = Uri.fromFile(file)
            // Enqueue media for upload immediately after successful storage
            try {
                val uploadQueueManager = UploadQueueManager.getInstance(context)
                uploadQueueManager.enqueueMedia(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            uri
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getMediaUri(filename: String): Uri? {
        val file = File(mediaStorageDir, "$filename.jpg")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun clearMediaStorage() {
        mediaStorageDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}
