package mentat.music.com.publicarbluesky.data.images

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

// En ImageUtils.kt
private const val TAG = "ImageUtils"
fun downloadImageToTempFile(
    context: Context,
    imageUrl: String,
    client: OkHttpClient
): File? {
    Log.e("IMAGE_UTILS_DEBUG", "downloadImageToTempFile FUE LLAMADA con URL: $imageUrl")
    Log.d(TAG, "Iniciando descarga de imagen desde URL: $imageUrl")
    if (imageUrl.isEmpty()) { // <--- SALIDA TEMPRANA 1 (debería loguear "URL de imagen vacía...")
        Log.w(TAG, "URL de imagen vacía, no se puede descargar.")
        return null
    }

    // AÑADIR UN LOG AQUÍ PARA VER SI LLEGAMOS A LA PETICIÓN HTTP
    Log.d(TAG, "Preparando request para OkHttp...")

    return try {
        val request = Request.Builder().url(imageUrl).build()
        // AÑADIR UN LOG ANTES DE EJECUTAR LA LLAMADA
        Log.d(TAG, "Ejecutando llamada OkHttp a: $imageUrl")
        val response = client.newCall(request).execute() // <--- PUNTO CRÍTICO

        if (!response.isSuccessful) {  // <--- SALIDA TEMPRANA 2 (debería loguear "Fallo en la descarga...")
            Log.e(
                TAG,
                "Fallo en la descarga. Código de respuesta: ${response.code}, URL: $imageUrl"
            )
            response.body?.close()
            return null
        }

        val body = response.body
        if (body == null) { // <--- SALIDA TEMPRANA 3 (debería loguear "Cuerpo de respuesta nulo...")
            Log.e(TAG, "Cuerpo de respuesta nulo para URL: $imageUrl")
            return null
        }

        val fileName = "hubble_img_${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        Log.d(TAG, "Guardando imagen descargada en: ${tempFile.absolutePath}")

        FileOutputStream(tempFile).use { outputStream ->
            body.byteStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.i(
            TAG,
            "Imagen guardada exitosamente: ${tempFile.absolutePath}, Tamaño: ${tempFile.length() / 1024} KB"
        )
        tempFile

    } catch (e: IOException) { // <--- MANEJO DE EXCEPCIÓN (debería loguear)
        Log.e(TAG, "IOException durante la descarga o guardado de $imageUrl: ${e.message}", e)
        null
    } catch (e: Exception) { // <--- MANEJO DE EXCEPCIÓN (debería loguear)
        Log.e(TAG, "Excepción general durante la descarga de $imageUrl: ${e.message}", e)
        null
    }
}


