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
import java.util.Calendar // <-- Importante para las horas
import java.util.concurrent.TimeUnit // <-- Importante para el delay
import androidx.work.Constraints // <-- Importante para re-programar
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest // <-- Importante para re-programar
import androidx.work.WorkManager // <-- Importante para re-programar


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

    // Dentro de HubblePostWorker.kt

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        // --- ¡¡LÍNEA MODIFICADA!! ---
        Log.i(TAG, "Worker iniciado. Tags: $tags") // Log para saber qué tipo de worker es

        var runResult: Result // Variable para guardar el resultado de esta ejecución

        // Un solo try-catch para toda la operación
        try {
            // --- 1. OBTENER IMAGEN ---
            Log.d(TAG, "Paso 1: Obteniendo imagen fresca del Hubble...")
            val processedImage = getFreshHubbleImageUseCase().getOrThrow()
            Log.d(TAG, "Imagen obtenida con éxito: ID ${processedImage.imageInfo.id}")


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

            val postResult = postToBlueskyUseCase(
                text = postText,
                imageId = imageInfo.id,
                imageBlob = blobObject,
                imageAltText = altText,
                facets = facets,
                langs = listOf("es")
            )
            postResult.getOrThrow()


            // --- 4. ÉXITO Y NOTIFICACIÓN ---
            Log.i(TAG, "¡Publicación completada con éxito!")
            showNotification(
                "Publicación Exitosa",
                "La imagen del Hubble se ha publicado."
            )

            // Guardamos que esta ejecución fue un éxito
            runResult = Result.success()

        } catch (e: Exception) {
            // CUALQUIER error de los pasos anteriores termina aquí
            Log.e(TAG, "Fallo en doWork: ${e.message}", e)
            showNotification("Fallo en la Publicación", e.message ?: "Ocurrió un error inesperado.")

            // Guardamos que esta ejecución falló
            runResult = Result.failure()
        }

        // --- 5. ¡¡BLOQUE MODIFICADO!! RE-PROGRAMAR SÓLO SI ES AUTOMÁTICO ---
        // Comprobamos la etiqueta para ver si debemos re-programar.
        if (tags.contains("automatic_chain")) {
            Log.d(TAG, "Es 'automatic_chain'. Programando la siguiente tarea...")
            scheduleNextWork()
        } else {
            // Si es un "manual_job", simplemente termina.
            Log.d(TAG, "Es 'manual_job'. No se re-programa la cadena.")
        }

        // Devolvemos el resultado de ESTA ejecución
        return runResult
    }

    // --- FUNCIONES DE LÓGICA DE POST ---

    // Tu función createFacets (basada en el código de tu antigua MainActivity)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFacets(postText: String, hubblePageUrl: String): List<Facet>? {
        val facets = mutableListOf<Facet>()
        if (hubblePageUrl.isNotBlank() && postText.contains(hubblePageUrl)) {
            createLinkFacet(postText, hubblePageUrl, hubblePageUrl)?.let { facets.add(it) }
        }
        facets.addAll(createTagFacets(postText))
        return facets.ifEmpty { null }
    }

    // Tu función de notificación (se queda igual)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este drawable
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }


    // --- NUEVAS FUNCIONES DE RE-PROGRAMACIÓN ---

    /**
     * Esta función se llama cuando el worker termina con ÉXITO.
     * Calcula cuál es la próxima hora (17 o 23) y programa
     * una nueva tarea (OneTimeWorkRequest) para ese momento.
     */
    // Dentro de HubblePostWorker.kt (esta es la función que va al final)

    private fun scheduleNextWork() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val delay: Long

        if (currentHour < 17) {
            // Si son menos de las 17h (ej: una prueba a las 14h), programar para las 17h
            delay = calculateDelay(17)
        } else if (currentHour < 23) {
            // Si son más de las 17h pero menos de las 23h (ej: se ejecutó a las 17h),
            // programar para las 23h (aprox 6 horas de espera)
            delay = calculateDelay(23)
        } else {
            // Si son más de las 23h (ej: se ejecutó a las 23h),
            // programar para las 17h de MAÑANA (aprox 18 horas de espera)
            delay = calculateDelay(17)
        }

        // Crear la próxima petición
        val nextWorkRequest = OneTimeWorkRequest.Builder(HubblePostWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("automatic_chain") // <-- ¡¡ESTA ES LA LÍNEA NUEVA!!
            .build()

        // --- ¡¡BLOQUE MODIFICADO!! ---
        // Encolar la próxima tarea REEMPLAZANDO la anterior en la cadena ÚNICA
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "hubble-post-chain", // El MISMO nombre único que en MyApplication
            ExistingWorkPolicy.REPLACE, // REEMPLAZAR la anterior
            nextWorkRequest
        )
        // (He modificado el log para que sea más claro)
        Log.i(TAG, "Próxima tarea (única) programada en $delay ms.")
    }

    /**
     * Función helper para calcular los milisegundos que faltan
     * desde AHORA hasta la 'targetHour' (17 o 23).
     * Si la hora ya pasó hoy, calcula para mañana.
     */
    private fun calculateDelay(targetHour: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var targetTime = calendar.timeInMillis

        if (targetTime <= now) {
            // Si ya ha pasado la hora objetivo de HOY, sumar 1 día
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            targetTime = calendar.timeInMillis
        }

        return targetTime - now // Devuelve la diferencia en milisegundos
    }
}