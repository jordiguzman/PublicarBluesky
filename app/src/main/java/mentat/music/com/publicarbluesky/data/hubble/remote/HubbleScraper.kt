package mentat.music.com.publicarbluesky.data.hubble.remote // O tu paquete

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mentat.music.com.publicarbluesky.data.hubble.model.HubbleSiteImage
import org.jsoup.Jsoup
import java.util.regex.Pattern

object HubbleScraper {

    // Podrías pasar maxPages como parámetro o definirlo como una constante aquí si es fijo
    // private const val DEFAULT_MAX_PAGES = 22

    suspend fun fetchRandomImageFromRandomPage(
        galleryBaseUrl: String, // ej: Constants.BASE_URL_HUBBLE
        maxPages: Int = 22 // Valor por defecto, puedes ajustarlo
    ): HubbleSiteImage? = withContext(Dispatchers.IO) {
        if (maxPages <= 0) {
            println("HubbleScraper: maxPages debe ser mayor que 0.")
            return@withContext null
        }

        val randomPageNumber = (1..maxPages).random()
        val pageUrl = if (randomPageNumber == 1) {
            galleryBaseUrl // Asumimos que la página 1 es la URL base de la galería
        } else {
            val cleanBase = galleryBaseUrl.removeSuffix("/")
            "$cleanBase/page/$randomPageNumber/"
        }

        println("HubbleScraper: Intentando scrapear página: $pageUrl")

        try {
            val document = Jsoup.connect(pageUrl).timeout(10000).get() // Timeout de 10s
            val scripts = document.getElementsByTag("script")
            var imagesJsonString: String? = null

            val jsonPattern = Pattern.compile("var images\\s*=\\s*(\\[.*?\\]);", Pattern.DOTALL)

            for (script in scripts) {
                val scriptContent = script.html()
                val matcher = jsonPattern.matcher(scriptContent)
                // ---- CORRECCIÓN AQUÍ ----
                if (matcher.find()) { // El if en su propia línea o después de un ; si estuviera en la misma
                    imagesJsonString = matcher.group(1) // El grupo 1 es el array JSON [...]
                    // println("HubbleScraper: JSON String extraído (longitud: ${imagesJsonString?.length})") // Descomentar para depurar JSON
                    break
                }
            }

            if (imagesJsonString == null) {
                println("HubbleScraper: No se pudo encontrar el JSON 'var images' en $pageUrl")
                return@withContext null
            }

            val gson = Gson()
            val imageListType = object : TypeToken<List<HubbleSiteImage>>() {}.type
            val imagesOnPage: List<HubbleSiteImage> = gson.fromJson(imagesJsonString, imageListType)

            if (imagesOnPage.isEmpty()) {
                println("HubbleScraper: Lista de imágenes vacía en $pageUrl después de parsear.")
                return@withContext null
            }

            val selectedImage = imagesOnPage.random()
            println("HubbleScraper: Imagen seleccionada de $pageUrl: ID=${selectedImage.id}, Título='${selectedImage.cleanedTitle}'")
            return@withContext selectedImage

        } catch (e: Exception) {
            println("HubbleScraper: Error durante el scraping o parsing de $pageUrl: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
}
