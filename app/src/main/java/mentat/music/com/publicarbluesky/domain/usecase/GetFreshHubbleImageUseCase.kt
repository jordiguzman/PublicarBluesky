package mentat.music.com.publicarbluesky.domain.usecase


import ProcessedHubbleImage
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mentat.music.com.publicarbluesky.constans.Constants
import mentat.music.com.publicarbluesky.data.hubble.remote.HubbleScraper
import mentat.music.com.publicarbluesky.data.images.downloadImageToTempFile
import okhttp3.OkHttpClient

// Define el TAG para esta clase si no lo has hecho ya
private const val TAG = "GetHubbleUseCase"

class GetFreshHubbleImageUseCase(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val MAX_HUBBLE_PAGES = 22

    /**
     * Intenta obtener información de una imagen aleatoria del Hubble y luego la descarga.
     * Intenta convertir la URL de descarga a HTTPS si es HTTP.
     * @return Result<ProcessedHubbleImage> que contiene el archivo de imagen descargado
     *         y la información de la imagen, o una excepción en caso de fallo.
     */
    suspend operator fun invoke(): Result<ProcessedHubbleImage> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando obtención de imagen del Hubble...")

        // --- 1. Obtener información de la imagen del Hubble ---
        Log.d(TAG, "Intentando obtener información de imagen del Hubble desde scraper...")
        val hubbleSiteImage = HubbleScraper.fetchRandomImageFromRandomPage(
            galleryBaseUrl = Constants.BASE_URL_HUBBLE,
            maxPages = MAX_HUBBLE_PAGES
        )

        if (hubbleSiteImage == null) {
            val errorMessage =
                "No se pudo obtener información de la imagen del Hubble desde el scraper."
            Log.e(TAG, errorMessage)
            return@withContext Result.failure(Exception(errorMessage))
        }

        // Preparar la URL para la descarga, intentando cambiar a HTTPS
        var imageUrlToDownload = hubbleSiteImage.screenResolutionImageUrl
        Log.d(TAG, "URL original obtenida del scraper: $imageUrlToDownload")

        if (imageUrlToDownload.startsWith("http://")) {
            Log.i(TAG, "La URL original es HTTP. Intentando cambiar a HTTPS.")
            imageUrlToDownload = imageUrlToDownload.replaceFirst("http://", "https://")
            Log.i(TAG, "URL modificada para descarga: $imageUrlToDownload")
        } else {
            Log.d(
                TAG,
                "La URL ya es HTTPS o no comienza con 'http://'. Usando tal cual: $imageUrlToDownload"
            )
        }

        Log.i(
            TAG,
            "Información de imagen: ID=${hubbleSiteImage.id}, Título='${hubbleSiteImage.cleanedTitle}'"
        )
        Log.d(TAG, "URL final que se usará para la descarga: $imageUrlToDownload")


        // --- 2. Descargar la imagen ---
        Log.d(TAG, "Descargando imagen desde: $imageUrlToDownload")
        val downloadedFile = downloadImageToTempFile(
            context = context.applicationContext,
            imageUrl = imageUrlToDownload, // Usar la URL (posiblemente) modificada
            client = okHttpClient
        )

        if (downloadedFile == null || !downloadedFile.exists()) {
            val errorMessage = "Fallo al descargar la imagen: $imageUrlToDownload"
            Log.e(TAG, errorMessage)
            // Aquí podrías querer loguear si la URL fue la original o la modificada a HTTPS
            // para ayudar a diagnosticar si el intento de HTTPS falló.
            // Por ejemplo, añadiendo a errorMessage si la URL fue cambiada.
            return@withContext Result.failure(Exception(errorMessage))
        }

        Log.i(
            TAG,
            "Imagen descargada exitosamente en: ${downloadedFile.absolutePath}, Tamaño: ${downloadedFile.length() / 1024} KB"
        )

        // --- 3. Devolver el resultado exitoso ---
        val processedImage =
            ProcessedHubbleImage(imageFile = downloadedFile, imageInfo = hubbleSiteImage)
        Result.success(processedImage)
    }
}