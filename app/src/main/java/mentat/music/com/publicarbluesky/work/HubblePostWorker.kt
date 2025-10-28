package mentat.music.com.publicarbluesky.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mentat.music.com.publicarbluesky.R
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
import mentat.music.com.publicarbluesky.data.bluesky.utils.createLinkFacet
import mentat.music.com.publicarbluesky.data.bluesky.utils.createTagFacets
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager


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

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker iniciado. Tags: $tags")

        var runResult: Result
        try {
            // --- 1. OBTENER IMAGEN ---
            Log.d(TAG, "Paso 1: Obteniendo imagen fresca del Hubble...")
            // 'processedImage' AHORA CONTIENE .width y .height
            val processedImage = getFreshHubbleImageUseCase().getOrThrow()
            Log.d(TAG, "Imagen obtenida con éxito: ID ${processedImage.imageInfo.id}, Dimensiones: ${processedImage.width}x${processedImage.height}")


            // --- 2. SUBIR IMAGEN ---
            Log.d(TAG, "Paso 2: Subiendo imagen a Bluesky...")
            val blobObject = uploadImageToBlueskyUseCase(processedImage.imageFile, "image/jpeg").getOrThrow()
            Log.d(TAG, "Imagen subida con éxito. CID: ${blobObject.ref.cid}")


            // --- 3. PUBLICAR POST ---
            Log.d(TAG, "Paso 3: Creando y publicando el post...")
            val imageInfo = processedImage.imageInfo
            val imageTitle = imageInfo.cleanedTitle ?: "Imagen del Telescopio Espacial Hubble"
            val hubblePageUrl = imageInfo.fullPageUrl
            var constructedPostText = imageTitle
            if (hubblePageUrl.isNotBlank()) {
                constructedPostText += "\n\nFuente: $hubblePageUrl"
            }
            constructedPostText += "\n\n#Hubble #esa #nasa"
            val postText = constructedPostText.take(300)
            val altText = imageInfo.cleanedTitle?.take(1000)
                ?: "Una imagen del espacio profundo capturada por el Hubble."

            val facets = createFacets(postText, hubblePageUrl)

            // --- ¡¡BLOQUE MODIFICADO!! ---
            // (Es normal que esto se ponga en rojo hasta el siguiente paso)
            val postResult = postToBlueskyUseCase(
                text = postText,
                imageId = imageInfo.id,
                imageBlob = blobObject,
                imageAltText = altText,
                // --- ¡¡LÍNEAS NUEVAS!! ---
                imageWidth = processedImage.width,
                imageHeight = processedImage.height,
                // --- FIN DE LÍNEAS NUEVAS ---
                facets = facets,
                langs = listOf("es")
            )
            // --- FIN DEL BLOQUE MODIFICADO ---
            postResult.getOrThrow()


            // --- 4. ÉXITO Y NOTIFICACIÓN ---
            Log.i(TAG, "¡Publicación completada con éxito!")
            showNotification(
                "Publicación Exitosa",
                "La imagen del Hubble se ha publicado."
            )

            runResult = Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Fallo en doWork: ${e.message}", e)
            showNotification("Fallo en la Publicación", e.message ?: "Ocurrió un error inesperado.")
            runResult = Result.failure()
        }

        // --- 5. RE-PROGRAMACIÓN (Se queda igual) ---
        if (tags.contains("automatic_chain")) {
            Log.d(TAG, "Es 'automatic_chain'. Programando la siguiente tarea...")
            scheduleNextWork()
        } else {
            Log.d(TAG, "Es 'manual_job'. No se re-programa la cadena.")
        }
        return runResult
    }

    // --- FUNCIONES DE LÓGICA DE POST (se quedan igual) ---
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFacets(postText: String, hubblePageUrl: String): List<Facet>? {
        val facets = mutableListOf<Facet>()
        if (hubblePageUrl.isNotBlank() && postText.contains(hubblePageUrl)) {
            createLinkFacet(postText, hubblePageUrl, hubblePageUrl)?.let { facets.add(it) }
        }
        facets.addAll(createTagFacets(postText))
        return facets.ifEmpty { null }
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notificaciones de Publicación",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre publicaciones automáticas del Hubble."
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }


    // --- FUNCIONES DE RE-PROGRAMACIÓN (se quedan igual) ---
    private fun scheduleNextWork() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val delay: Long
        if (currentHour < 17) {
            delay = calculateDelay(17)
        } else if (currentHour < 23) {
            delay = calculateDelay(23)
        } else {
            delay = calculateDelay(17)
        }
        val nextWorkRequest = OneTimeWorkRequest.Builder(HubblePostWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("automatic_chain")
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "hubble-post-chain",
            ExistingWorkPolicy.REPLACE,
            nextWorkRequest
        )
        Log.i(TAG, "Próxima tarea (única) programada en $delay ms.")
    }

    private fun calculateDelay(targetHour: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var targetTime = calendar.timeInMillis
        if (targetTime <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            targetTime = calendar.timeInMillis
        }
        return targetTime - now
    }
}