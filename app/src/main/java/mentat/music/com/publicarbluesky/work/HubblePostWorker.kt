package mentat.music.com.publicarbluesky.work

import mentat.music.com.publicarbluesky.domain.model.ProcessedHubbleImage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mentat.music.com.publicarbluesky.R
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import kotlin.getOrThrow
import mentat.music.com.publicarbluesky.helpers.createFacets



class HubblePostWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase,
    private val uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase,
    private val postToBlueskyUseCase: PostToBlueskyUseCase
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val TAG = "HubblePostWorker"
        const val NOTIFICATION_CHANNEL_ID = "hubble_post_channel"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker iniciado. Comenzando proceso de publicación automática.")

        return try { // El try-catch envuelve todo y devuelve el resultado final
            // 1. Obtener imagen fresca del Hubble        Log.d(TAG, "Paso 1: Obteniendo imagen fresca del Hubble...")
            val imageResult = getFreshHubbleImageUseCase()

            // Usamos fold para manejar el éxito y el fracaso de forma segura
            imageResult.fold(
                onSuccess = { processedImage: ProcessedHubbleImage ->
                    // Si llegamos aquí, tenemos una imagen válida. Continuamos con la lógica.
                    Log.d(TAG, "Imagen obtenida con éxito: ID ${processedImage.imageInfo.id}")
                    // Llamamos a una nueva función que contiene el resto de los pasos
                    // PERO NO DEVOLVEMOS SU RESULTADO AQUÍ
                },
                onFailure = { error: Throwable ->
                    // Si el UseCase falló, lanzamos una excepción para que el CATCH la recoja.
                    throw error
                }
            )

            // Si el fold tuvo éxito, la imagen está en imageResult.getOrThrow()
            // y podemos continuar con el siguiente paso.
            val successfulImage = imageResult.getOrThrow()
            publishImage(successfulImage) // Llamamos a la función que hace el resto y devuelve el Result final

        } catch (e: Exception) {
            // CUALQUIER error (del getFresh, del publish, etc.) será capturado aquí
            Log.e(TAG, "Fallo en doWork: ${e.message}", e)
            showNotification("Fallo en la Publicación", e.message ?: "Ocurrió un error inesperado.")
            Result.failure() // Devolvemos un fallo general
        }
    }


    // --- NUEVA FUNCIÓN EXTRAÍDA PARA CLARIDAD ---
    private suspend fun publishImage(processedImage: ProcessedHubbleImage): Result {
        val imageInfo = processedImage.imageInfo




        // 2. Subir la imagen a Bluesky para obtener el Blob
        Log.d(TAG, "Paso 2: Subiendo imagen a Bluesky...")
        val uploadResult = uploadImageToBlueskyUseCase(processedImage.imageFile, "image/jpeg")

        val blobObject = uploadResult.getOrNull()
            ?: run {
                val errorMsg =
                    uploadResult.exceptionOrNull()?.message ?: "Error desconocido al subir imagen."
                Log.e(TAG, "Fallo al subir imagen a Bluesky: $errorMsg")
                showNotification(
                    "Fallo en la publicación",
                    "Error al subir la imagen al servidor de Bluesky."
                )
                return Result.failure()
            }
        Log.d(TAG, "Imagen subida con éxito. CID: ${blobObject.ref.cid}")

        // 3. Crear el post con el texto, la imagen y los facets
        Log.d(TAG, "Paso 3: Creando y publicando el post...")
        val postText = "Imagen del Telescopio Hubble\n\n#Hubble #Espacio #Astronomía"
        val altText = "Una imagen capturada por el Telescopio Espacial Hubble."
        val facets = createFacets(postText) // <--- DESCOMENTAR


        val postResult = postToBlueskyUseCase(
            text = postText,
            imageId = imageInfo.id, // ¡Importante para que se guarde!
            imageBlob = blobObject,
            imageAltText = altText,
            facets = facets,
            langs = listOf("es")
        )

        // 4. Comprobar resultado final y enviar notificación
        return postResult.fold(
            onSuccess = { uri ->
                Log.i(TAG, "¡Publicación completada con éxito! URI: $uri")
                showNotification(
                    "Publicación Exitosa",
                    "La imagen del Hubble de hoy se ha publicado."
                )
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Fallo al crear el post en Bluesky: ${error.message}", error)
                showNotification(
                    "Fallo en la publicación",
                    "Error final al publicar el post en Bluesky."
                )
                Result.failure()
            }
        )
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (necesario para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notificaciones de Publicación",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Notificaciones sobre el estado de las publicaciones automáticas del Hubble."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este drawable
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}