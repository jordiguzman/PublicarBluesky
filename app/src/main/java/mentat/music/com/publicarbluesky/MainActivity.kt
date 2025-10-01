package mentat.music.com.publicarbluesky

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mentat.music.com.publicarbluesky.constans.Constants
import mentat.music.com.publicarbluesky.data.bluesky.model.BlobObject
import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import mentat.music.com.publicarbluesky.data.bluesky.remote.client.BlueskyClient
import mentat.music.com.publicarbluesky.data.bluesky.utils.createLinkFacet
import mentat.music.com.publicarbluesky.data.bluesky.utils.createTagFacets
import mentat.music.com.publicarbluesky.domain.repository.PublishedImageIdRepository
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.InMemorySessionManager
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.SessionManager
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import mentat.music.com.publicarbluesky.ui.features.post.MainScreen
import okhttp3.OkHttpClient

// Importa tus clases de SessionManager si están en otro paquete
class MainActivity : ComponentActivity() {

    private lateinit var getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase
    private lateinit var postToBlueskyUseCase: PostToBlueskyUseCase
    private lateinit var uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var blueskyApi: BlueskyApi
    private lateinit var sessionManager: SessionManager // <--- AÑADIR DECLARACIÓN
    private lateinit var publishedImageIdRepository: PublishedImageIdRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar dependencias de red
        okHttpClient = OkHttpClient()
        blueskyApi = BlueskyClient.api
        publishedImageIdRepository =
            PublishedImageIdRepository(applicationContext) // <--- NUEVA INICIALIZACIÓN

        // 1.1. Inicializar SessionManager
        // Asegúrate de que InMemorySessionManager está accesible aquí (misma clase o importado)
        val inMemorySessionManager = InMemorySessionManager(blueskyApi)
        // Establecer las credenciales para que el SessionManager pueda crear sesiones
        // ESTO ES PARA PRUEBAS. En una app real, obtendrías esto de un login seguro.
        inMemorySessionManager.setCredentials(
            Constants.BLUESKY_USERNAME,
            Constants.BLUESKY_APP_PASSWORD
        )
        sessionManager = inMemorySessionManager


        // 2. Inicializar UseCases
        getFreshHubbleImageUseCase = GetFreshHubbleImageUseCase(
            context = applicationContext,
            okHttpClient = okHttpClient,
            publishedImageIdRepository = publishedImageIdRepository,
            maxScrapingAttempts = 10 // Puedes ajustar este valor según tus necesidades
            // Nota: GetFreshHubbleImageUseCase no parece necesitar SessionManager,
            // ya que obtiene imágenes de una API pública. Si sí lo necesita, añádelo.
        )

        postToBlueskyUseCase = PostToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager // <--- PASAR sessionManager
        )

        // Asumimos que UploadImageToBlueskyUseCase también necesita SessionManager
        // para obtener el token de autorización. Si tu UploadImageToBlueskyUseCase
        // actual no lo tiene, deberás modificarlo para que lo acepte y lo use.
        uploadImageToBlueskyUseCase = UploadImageToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager // <--- PASAR sessionManager
        )

        // 3. Configurar la UI con Compose
        setContent {
            Scaffold { innerPadding ->
                MainScreen(
                    modifier = Modifier.padding(innerPadding),
                    onPostTextClick = { textToPost ->
                        // Ya no pasamos identifier y appPassword aquí,
                        // PostToBlueskyUseCase los obtiene del SessionManager
                        callPostTextToBlueskyUseCase(
                            text = textToPost
                        )
                    },
                    onFetchAndProcessHubbleImageClick = {
                        callGetFreshHubbleImageAndUploadToBluesky()
                    }
                )
            }
        }
    }

    // En MainActivity.kt
// Asegúrate de tener estas importaciones al principio de tu MainActivity.kt:
// import mentat.music.com.publicarbluesky.data.bluesky.utils.createLinkFacet
// import mentat.music.com.publicarbluesky.data.bluesky.utils.createTagFacets
// import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
    @RequiresApi(Build.VERSION_CODES.O)
    private fun callGetFreshHubbleImageAndUploadToBluesky() {
        lifecycleScope.launch {
            Log.d("MainActivity", "Botón 'Obtener Imagen Hubble' presionado...")
            // Aquí podrías mostrar un indicador de carga en la UI

            val hubbleResult = getFreshHubbleImageUseCase()

            hubbleResult.fold(
                onSuccess = { processedHubbleImage ->
                    Log.i(
                        "MainActivity",
                        "Imagen Hubble obtenida: ID ${processedHubbleImage.imageInfo.id}"
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "Imagen Hubble lista: ID ${processedHubbleImage.imageInfo.id}. Subiendo...",
                        Toast.LENGTH_SHORT
                    ).show()

                    val imageFile = processedHubbleImage.imageFile
                    val mimeType = "image/jpeg" // Asumimos JPEG por ahora

                    val uploadResult = uploadImageToBlueskyUseCase(
                        imageFile = imageFile,
                        mimeType = mimeType
                    )

                    uploadResult.fold(
                        onSuccess = { blobObject ->
                            val uploadSuccessMsg =
                                "¡Imagen subida a Bluesky! CID: ${blobObject.ref.cid}"
                            Log.i("MainActivity", uploadSuccessMsg)
                            Toast.makeText(
                                this@MainActivity,
                                "$uploadSuccessMsg. Publicando...",
                                Toast.LENGTH_SHORT
                            ).show()

                            // --- Construcción del texto del post ---
                            val imageTitle =
                                processedHubbleImage.imageInfo.cleanedTitle
                                    ?: "Imagen del Telescopio Espacial Hubble"

                            val hubblePageUrl =
                                processedHubbleImage.imageInfo.fullPageUrl

                            var constructedPostText = imageTitle
                            if (hubblePageUrl.isNotBlank()) {
                                constructedPostText += "\n\nFuente: $hubblePageUrl"
                            }

                            // --- AÑADE ALGUNOS HASHTAGS DE EJEMPLO AL TEXTO ---
                            // Puedes obtenerlos de otro lugar, o añadirlos manualmente para probar
                            constructedPostText += "\n\n#Hubble #esa #nasa"
                            // --------------------------------------------------

                            // Trunca el texto DESPUÉS de añadir la URL y hashtags.
                            // Asegúrate de que los hashtags importantes no se corten si están cerca del final.
                            val postTextForBluesky = constructedPostText.take(300)

                            val altTextForBluesky =
                                processedHubbleImage.imageInfo.cleanedTitle?.take(1000)
                                    ?: "Una imagen del espacio profundo capturada por el Hubble."

                            // --- INICIO: Creación de todos los Facets ---
                            val facets =
                                mutableListOf<Facet>()

                            // 1. Crear Facets para Enlaces
                            if (hubblePageUrl.isNotBlank() && postTextForBluesky.contains(
                                    hubblePageUrl
                                )
                            ) {
                                createLinkFacet(
                                    textContent = postTextForBluesky,
                                    linkText = hubblePageUrl,
                                    linkUrl = hubblePageUrl
                                )?.let { facet ->
                                    facets.add(facet)
                                    Log.d(
                                        "MainActivity",
                                        "Facet de enlace CREADO para URL: $hubblePageUrl. Facet: $facet"
                                    )
                                }
                            } else if (hubblePageUrl.isNotBlank()) {
                                Log.w(
                                    "MainActivity",
                                    "La URL '$hubblePageUrl' (linkText) no se encontró en el texto final del post '$postTextForBluesky'. NO se creará facet de enlace para ella."
                                )
                            }

                            // 2. Crear Facets para Hashtags
                            // Usamos la ruta completa a la función por si no tienes la importación directa aún
                            val tagFacets =
                                createTagFacets(
                                    postTextForBluesky
                                )
                            if (tagFacets.isNotEmpty()) {
                                facets.addAll(tagFacets)
                                Log.d(
                                    "MainActivity",
                                    "Facets de Tag CREADOS: ${tagFacets.size} tags encontrados."
                                )
                            } else {
                                Log.d(
                                    "MainActivity",
                                    "No se encontraron hashtags en '$postTextForBluesky' para crear facets."
                                )
                            }
                            // --- FIN DE LA CREACIÓN DE FACETS ---

                            Log.d(
                                "MainActivity",
                                "Preparando para postear. Texto FINAL: '$postTextForBluesky', Alt text: '$altTextForBluesky', CID: ${blobObject.ref.cid}, Facets: $facets"
                            )

                            callPostToBlueskyWithImage(
                                text = postTextForBluesky,
                                imageBlob = blobObject,
                                imageAltText = altTextForBluesky,
                                facets = facets.ifEmpty { null }
                            )
                        },
                        onFailure = { uploadException ->
                            val uploadErrorMsg =
                                "Error al subir imagen a Bluesky: ${uploadException.message}"
                            Log.e("MainActivity", uploadErrorMsg, uploadException)
                            Toast.makeText(this@MainActivity, uploadErrorMsg, Toast.LENGTH_LONG)
                                .show()
                        }
                    )
                },
                onFailure = { hubbleException ->
                    val hubbleErrorMsg =
                        "Error al obtener imagen del Hubble: ${hubbleException.message}"
                    Log.e("MainActivity", hubbleErrorMsg, hubbleException)
                    Toast.makeText(this@MainActivity, hubbleErrorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostToBlueskyWithImage(
        text: String,
        imageBlob: BlobObject,
        imageAltText: String,
        facets: List<mentat.music.com.publicarbluesky.data.bluesky.model.Facet>? // <--- AÑADIDO EL PARÁMETRO facets
    ) {
        lifecycleScope.launch {
            Log.d(
                "MainActivity",
                "Llamando a PostToBlueskyUseCase (con imagen)... Texto: '$text', Alt: '$imageAltText', Facets: $facets"
            )
            // Aquí podrías mostrar un indicador de carga si no está visible

            val result = postToBlueskyUseCase( // Llamada al invoke del UseCase
                text = text,
                imageBlob = imageBlob,
                imageAltText = imageAltText,
                facets = facets, // <--- PASANDO facets AL USE CASE
                langs = listOf("es") // O el idioma que prefieras por defecto
            )

            result.fold(
                onSuccess = { successMessage ->
                    Log.i("MainActivity", "Post con imagen exitoso: $successMessage")
                    Toast.makeText(this@MainActivity, successMessage, Toast.LENGTH_LONG).show()
                },
                onFailure = { exception ->
                    val errorMsg = "Error al postear con imagen: ${exception.message}"
                    Log.e("MainActivity", errorMsg, exception)
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
            // Aquí podrías ocultar el indicador de carga
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostTextToBlueskyUseCase(
        // identifier y appPassword ya no son necesarios aquí
        text: String
    ) {
        lifecycleScope.launch {
            Log.d("MainActivity", "Llamando a PostToBlueskyUseCase (solo texto)...")
            val result = postToBlueskyUseCase( // Llamada al invoke del UseCase
                text = text,
                imageBlob = null,
                imageAltText = null,
                facets = null // Todavía no estamos construyendo facets
            )

            result.fold(
                onSuccess = { successMessage ->
                    Log.i("MainActivity", "Post de texto exitoso: $successMessage")
                    Toast.makeText(this@MainActivity, successMessage, Toast.LENGTH_LONG).show()
                },
                onFailure = { exception ->
                    val errorMsg = "Error al postear texto: ${exception.message}"
                    Log.e("MainActivity", errorMsg, exception)
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}