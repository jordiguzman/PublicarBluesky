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
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import mentat.music.com.publicarbluesky.data.bluesky.remote.client.BlueskyClient
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import mentat.music.com.publicarbluesky.ui.features.post.MainScreen
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private lateinit var getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase
    private lateinit var postToBlueskyUseCase: PostToBlueskyUseCase
    private lateinit var uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var blueskyApi: BlueskyApi

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar dependencias
        okHttpClient = OkHttpClient()
        blueskyApi = BlueskyClient.api

        // 2. Inicializar UseCases
        getFreshHubbleImageUseCase = GetFreshHubbleImageUseCase(
            context = applicationContext,
            okHttpClient = okHttpClient
        )

        postToBlueskyUseCase = PostToBlueskyUseCase(
            blueskyApi = blueskyApi
        )

        uploadImageToBlueskyUseCase = UploadImageToBlueskyUseCase(
            blueskyApi = blueskyApi
        )

        // 3. Configurar la UI con Compose
        setContent {
            Scaffold { innerPadding ->
                MainScreen(
                    modifier = Modifier.padding(innerPadding),
                    onPostTextClick = { textToPost ->
                        callPostTextToBlueskyUseCase(
                            identifier = Constants.BLUESKY_USERNAME,
                            appPassword = Constants.BLUESKY_APP_PASSWORD,
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun callGetFreshHubbleImageAndUploadToBluesky() {
        lifecycleScope.launch {
            Log.d("MainActivity", "Botón 'Obtener Imagen Hubble' presionado. Iniciando flujo...")
            // Aquí podrías mostrar un indicador de carga en la UI

            val hubbleResult = getFreshHubbleImageUseCase()

            hubbleResult.fold(
                onSuccess = { processedHubbleImage ->
                    val hubbleSuccessMsg =
                        "Imagen Hubble obtenida: ${processedHubbleImage.imageInfo.id}. Guardada en: ${processedHubbleImage.imageFile.absolutePath}"
                    Log.i("MainActivity", hubbleSuccessMsg)
                    Log.d("MainActivity", "Título: ${processedHubbleImage.imageInfo.cleanedTitle}")
                    Log.d(
                        "MainActivity",
                        "Tamaño: ${processedHubbleImage.imageFile.length() / 1024} KB"
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "Imagen Hubble lista: ${processedHubbleImage.imageInfo.id}. Subiendo...",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(
                        "MainActivity",
                        "Intentando subir imagen '${processedHubbleImage.imageInfo.id}' a Bluesky..."
                    )
                    val imageFile = processedHubbleImage.imageFile
                    val mimeType = "image/jpeg" // Asumimos JPEG por ahora

                    val uploadResult = uploadImageToBlueskyUseCase(
                        imageFile = imageFile,
                        mimeType = mimeType,
                        identifier = Constants.BLUESKY_USERNAME,
                        appPassword = Constants.BLUESKY_APP_PASSWORD
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

                            // Determinar el texto del post y el texto alternativo
                            val postText =
                                processedHubbleImage.imageInfo.cleanedTitle?.take(300)
                                    ?: "Imagen del Telescopio Espacial Hubble" // Bluesky tiene límite de 300 caracteres para el texto del post.
                            val altText =
                                processedHubbleImage.imageInfo.cleanedTitle?.take(1000)
                                    ?: "Una imagen del espacio profundo capturada por el Hubble." // El alt text puede ser más largo.

                            Log.d(
                                "MainActivity",
                                "Preparando para postear. Texto: '$postText', Alt text: '$altText', CID: ${blobObject.ref.cid}"
                            )

                            callPostToBlueskyWithImage(
                                identifier = Constants.BLUESKY_USERNAME,
                                appPassword = Constants.BLUESKY_APP_PASSWORD,
                                text = postText,
                                imageBlob = blobObject,
                                imageAltText = altText
                            )
                        },
                        onFailure = { uploadException ->
                            val uploadErrorMsg =
                                "Error al subir imagen a Bluesky: ${uploadException.message}"
                            Log.e("MainActivity", uploadErrorMsg, uploadException)
                            Toast.makeText(this@MainActivity, uploadErrorMsg, Toast.LENGTH_LONG)
                                .show()
                            // Aquí podrías ocultar el indicador de carga
                        }
                    )
                },
                onFailure = { hubbleException ->
                    val hubbleErrorMsg =
                        "Error al obtener imagen del Hubble: ${hubbleException.message}"
                    Log.e("MainActivity", hubbleErrorMsg, hubbleException)
                    Toast.makeText(this@MainActivity, hubbleErrorMsg, Toast.LENGTH_LONG).show()
                    // Aquí podrías ocultar el indicador de carga
                }
            )
        }
    }

    /**
     * Llama al PostToBlueskyUseCase para publicar texto CON una imagen.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostToBlueskyWithImage(
        identifier: String,
        appPassword: String,
        text: String,
        imageBlob: BlobObject,
        imageAltText: String
    ) {
        lifecycleScope.launch {
            Log.d(
                "MainActivity",
                "Llamando a PostToBlueskyUseCase (con imagen). Texto: '$text', CID: ${imageBlob.ref.cid}, Alt: '$imageAltText'"
            )
            // Aquí podrías mostrar un indicador de carga si no está visible

            val result = postToBlueskyUseCase(
                identifier = identifier,
                appPassword = appPassword,
                text = text,
                imageBlob = imageBlob,
                imageAltText = imageAltText
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

    /**
     * Llama al PostToBlueskyUseCase para publicar solo texto.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostTextToBlueskyUseCase(
        identifier: String,
        appPassword: String,
        text: String
    ) {
        lifecycleScope.launch {
            Log.d(
                "MainActivity",
                "Botón 'Postear Texto' presionado. Llamando a PostToBlueskyUseCase..."
            )
            // Aquí podrías mostrar un indicador de carga

            val result = postToBlueskyUseCase(
                identifier = identifier,
                appPassword = appPassword,
                text = text,
                imageBlob = null,
                imageAltText = null
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
            // Aquí podrías ocultar el indicador de carga
        }
    }
}