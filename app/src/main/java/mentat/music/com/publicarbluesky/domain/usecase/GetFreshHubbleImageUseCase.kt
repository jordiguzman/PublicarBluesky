package mentat.music.com.publicarbluesky.domain.usecase


import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mentat.music.com.publicarbluesky.constans.Constants
import mentat.music.com.publicarbluesky.data.hubble.model.HubbleSiteImage
import mentat.music.com.publicarbluesky.data.hubble.remote.HubbleScraper
import mentat.music.com.publicarbluesky.data.images.downloadImageToTempFile
import mentat.music.com.publicarbluesky.domain.model.ProcessedHubbleImage
import mentat.music.com.publicarbluesky.domain.repository.PublishedImageIdRepository
import okhttp3.OkHttpClient

private const val TAG = "GetHubbleUseCase"

class GetFreshHubbleImageUseCase(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val publishedImageIdRepository: PublishedImageIdRepository,
    private val maxScrapingAttempts: Int = 10 // Número de intentos para encontrar una imagen no publicada (ajusta según veas necesario)
) {
    // Si HubbleScraper usa un valor fijo o lo obtiene de Constants, puedes referenciarlo o mantenerlo consistente
    private val MAX_HUBBLE_PAGES_FOR_SCRAPER = 22 // O el valor que usa/necesita HubbleScraper

    suspend operator fun invoke(): Result<ProcessedHubbleImage> = withContext(Dispatchers.IO) {
        Log.d(
            TAG,
            "Iniciando obtención de imagen del Hubble. Máximos intentos de scraping: $maxScrapingAttempts"
        )

        var attempts = 0
        var hubbleSiteImageToProcess: HubbleSiteImage? =
            null // Para guardar la imagen que SÍ vamos a procesar

        // --- Bucle para encontrar una imagen no publicada ---
        while (attempts < maxScrapingAttempts) {
            attempts++
            Log.i(
                TAG,
                "Intento de scraping #$attempts de $maxScrapingAttempts para encontrar imagen no publicada..."
            )

            val candidateImage: HubbleSiteImage? = try {
                // LLAMAMOS A TU HUBBLESCRAPER AQUÍ, DENTRO DEL BUCLE
                HubbleScraper.fetchRandomImageFromRandomPage(
                    galleryBaseUrl = Constants.BASE_URL_HUBBLE,
                    maxPages = MAX_HUBBLE_PAGES_FOR_SCRAPER
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Intento #$attempts: Excepción al obtener información de imagen del scraper: ${e.message}",
                    e
                )
                if (attempts >= maxScrapingAttempts) { // Si es el último intento y falla el scraper
                    return@withContext Result.failure(
                        Exception(
                            "Fallo crítico del scraper en el último intento (#$attempts): ${e.message}",
                            e
                        )
                    )
                }
                Log.w(TAG, "Intento #$attempts: Scraper falló, reintentando si quedan intentos...")
                continue // Siguiente intento del bucle while
            }

            if (candidateImage == null) {
                Log.w(TAG, "Intento #$attempts: HubbleScraper no devolvió ninguna imagen.")
                // Si el scraper consistentemente no devuelve imágenes, podríamos querer fallar antes.
                // Por ahora, continuamos hasta agotar intentos.
                if (attempts >= maxScrapingAttempts) {
                    break // Salir del bucle si se agotan los intentos y el scraper no da nada
                }
                continue // Siguiente intento
            }

            // --- TU CÓDIGO ACTUAL (OBTENER ID Y TÍTULO) ---
            val imageId = candidateImage.id
            val imageTitle = candidateImage.cleanedTitle
            Log.d(
                TAG,
                "Intento #$attempts: Imagen candidata obtenida: ID=$imageId, Título='$imageTitle'"
            )
            // --- FIN DE TU CÓDIGO ACTUAL ---

            // --- INTEGRACIÓN DE LA COMPROBACIÓN DEL REPOSITORIO ---
            if (!publishedImageIdRepository.hasBeenPublished(imageId)) {
                Log.i(
                    TAG,
                    "¡ÉXITO en intento #$attempts! Imagen ID '$imageId' NO ha sido publicada. Seleccionada."
                )
                hubbleSiteImageToProcess =
                    candidateImage // Guardamos la imagen encontrada para procesarla después del bucle
                break // Salir del bucle while, hemos encontrado una imagen para procesar
            } else {
                Log.i(
                    TAG,
                    "Intento #$attempts: Imagen ID '$imageId' YA FUE PUBLICADA. Buscando otra..."
                )
                // El bucle continuará para el siguiente intento si no se han agotado los 'attempts'.
            }
            // --- FIN DE LA INTEGRACIÓN ---
        }

        // --- Comprobar si se encontró una imagen después de los intentos ---
        if (hubbleSiteImageToProcess == null) {
            val errorMessage =
                "No se pudo encontrar una imagen del Hubble sin publicar después de $maxScrapingAttempts intentos."
            Log.w(TAG, errorMessage)
            return@withContext Result.failure(Exception(errorMessage))
        }

        // --- Descargar la imagen seleccionada (que sabemos que no ha sido publicada) ---
        // Usamos hubbleSiteImageToProcess!! porque hemos comprobado que no es null justo arriba.
        Log.i(
            TAG,
            "Procediendo a descargar la imagen ID '${hubbleSiteImageToProcess!!.id}' (Título: '${hubbleSiteImageToProcess!!.cleanedTitle}')"
        )
        var imageUrlToDownload = hubbleSiteImageToProcess!!.screenResolutionImageUrl

        if (imageUrlToDownload.startsWith("http://")) {
            imageUrlToDownload = imageUrlToDownload.replaceFirst("http://", "https://")
        }

        Log.d(TAG, "Descargando imagen desde: $imageUrlToDownload")
        val downloadedFile = try {
            downloadImageToTempFile(
                context = context.applicationContext,
                imageUrl = imageUrlToDownload,
                client = okHttpClient
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Excepción al descargar la imagen ID '${hubbleSiteImageToProcess!!.id}': ${e.message}",
                e
            )
            return@withContext Result.failure(
                Exception(
                    "Fallo al descargar la imagen '$imageUrlToDownload': ${e.message}",
                    e
                )
            )
        }

        if (downloadedFile == null || !downloadedFile.exists()) {
            val errorMessage =
                "Fallo al descargar la imagen (archivo nulo o no existe): $imageUrlToDownload"
            Log.e(TAG, errorMessage)
            return@withContext Result.failure(Exception(errorMessage))
        }

        Log.i(
            TAG,
            "Imagen ID '${hubbleSiteImageToProcess!!.id}' descargada exitosamente en: ${downloadedFile.absolutePath}"
        )

        // --- Devolver el resultado exitoso ---
        // Usamos hubbleSiteImageToProcess!! porque ya hemos verificado que no es null.
        val processedImage =
            ProcessedHubbleImage(imageFile = downloadedFile, imageInfo = hubbleSiteImageToProcess!!)
        Result.success(processedImage)
    }
}