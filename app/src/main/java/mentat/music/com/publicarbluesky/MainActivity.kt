package mentat.music.com.publicarbluesky

import PublishedImageIdRepository
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
    private val publishedImageIdRepository by lazy {
        PublishedImageIdRepository(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar dependencias de red
        okHttpClient = OkHttpClient()
        blueskyApi = BlueskyClient.api

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
            okHttpClient = okHttpClient
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

                    // --- INICIO DE LA COMPROBACIÓN ---
                    if (publishedImageIdRepository.hasBeenPublished(processedHubbleImage.imageInfo.id)) {
                        val alreadyPublishedMsg =
                            "Publicación omitida: La imagen de hoy (ID: ${processedHubbleImage.imageInfo.id}) ya ha sido publicada."
                        Log.d("MainActivity", alreadyPublishedMsg)
                        Toast.makeText(this@MainActivity, alreadyPublishedMsg, Toast.LENGTH_LONG)
                            .show()
                        return@launch // Detiene la ejecución de la función aquí.
                    }
                    // --- FIN DE LA COMPROBACIÓN ---


                    Toast.makeText(
                        this@MainActivity,
                        "Imagen Hubble nueva: ID ${processedHubbleImage.imageInfo.id}. Subiendo...",
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

                            constructedPostText += "\n\n#Hubble #esa #nasa"
                            val postTextForBluesky = constructedPostText.take(300)
                            val altTextForBluesky =
                                processedHubbleImage.imageInfo.cleanedTitle?.take(1000)
                                    ?: "Una imagen del espacio profundo capturada por el Hubble."

                            val facets = mutableListOf<Facet>()
                            if (hubblePageUrl.isNotBlank() && postTextForBluesky.contains(
                                    hubblePageUrl
                                )
                            ) {
                                createLinkFacet(
                                    textContent = postTextForBluesky,
                                    linkText = hubblePageUrl,
                                    linkUrl = hubblePageUrl
                                )?.let { facets.add(it) }
                            }
                            val tagFacets = createTagFacets(postTextForBluesky)
                            if (tagFacets.isNotEmpty()) {
                                facets.addAll(tagFacets)
                            }
                            Log.d(
                                "MainActivity",
                                "Preparando para postear. Texto FINAL: '$postTextForBluesky', Alt text: '$altTextForBluesky', CID: ${blobObject.ref.cid}, Facets: $facets"
                            )

                            // --- GUARDAR ID PUBLICADO ---
                            // Lo hacemos justo ANTES de llamar a la publicación final.
                            // Si la app se cierra aquí, en el próximo intento se omitirá, lo cual es seguro.
                            publishedImageIdRepository.addPublishedId(processedHubbleImage.imageInfo.id)
                            Log.i(
                                "MainActivity",
                                "ID ${processedHubbleImage.imageInfo.id} guardado como publicado."
                            )
                            // --- FIN DE GUARDAR ID ---

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