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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import mentat.music.com.publicarbluesky.constans.Constants
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import mentat.music.com.publicarbluesky.data.bluesky.remote.client.BlueskyClient
import mentat.music.com.publicarbluesky.domain.repository.PublishedImageIdRepository
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.InMemorySessionManager
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.SessionManager
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import mentat.music.com.publicarbluesky.ui.features.post.MainScreen
import mentat.music.com.publicarbluesky.work.HubblePostWorker
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    // --- Las dependencias que ya no se usan directamente aquí podrían eliminarse,
    // --- pero las mantenemos por ahora para la función de postear texto.
    private lateinit var getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase
    private lateinit var postToBlueskyUseCase: PostToBlueskyUseCase
    private lateinit var uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var blueskyApi: BlueskyApi
    private lateinit var sessionManager: SessionManager
    private lateinit var publishedImageIdRepository: PublishedImageIdRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIALIZACIÓN DE DEPENDENCIAS (se mantiene para la función de postear texto) ---
        okHttpClient = OkHttpClient()
        blueskyApi = BlueskyClient.api
        publishedImageIdRepository = PublishedImageIdRepository(applicationContext)

        val inMemorySessionManager = InMemorySessionManager(blueskyApi)
        inMemorySessionManager.setCredentials(
            Constants.BLUESKY_USERNAME,
            Constants.BLUESKY_APP_PASSWORD
        )
        sessionManager = inMemorySessionManager

        getFreshHubbleImageUseCase = GetFreshHubbleImageUseCase(
            context = applicationContext,
            okHttpClient = okHttpClient,
            publishedImageIdRepository = publishedImageIdRepository,
            maxScrapingAttempts = 10
        )

        postToBlueskyUseCase = PostToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager,
            publishedImageIdRepository = publishedImageIdRepository
        )

        uploadImageToBlueskyUseCase = UploadImageToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager
        )
        // --- FIN DE LA INICIALIZACIÓN ---


        // 3. Configurar la UI con Compose
        setContent {
            Scaffold { innerPadding ->
                MainScreen(
                    modifier = Modifier.padding(innerPadding),
                    onPostTextClick = { textToPost ->
                        callPostTextToBlueskyUseCase(
                            text = textToPost
                        )
                    },
                    // --- INICIO DE LA MODIFICACIÓN CLAVE ---
                    onFetchAndProcessHubbleImageClick = {
                        Log.d(
                            "MainActivity",
                            "Botón 'Obtener y Publicar' presionado. Delegando a WorkManager."
                        )

                        // 1. Creamos una petición para ejecutar nuestro worker una sola vez.
                        val hubbleWorkRequest = OneTimeWorkRequestBuilder<HubblePostWorker>()
                            .build()

                        // 2. Obtenemos la instancia de WorkManager y ponemos la petición en cola.
                        WorkManager.getInstance(applicationContext).enqueue(hubbleWorkRequest)

                        // 3. Mostramos un mensaje inmediato al usuario.
                        Toast.makeText(
                            applicationContext,
                            "Publicación en segundo plano iniciada...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // --- FIN DE LA MODIFICACIÓN CLAVE ---
                )
            }
        }
    }

    // =========================================================================================
    //  LAS FUNCIONES "callGetFreshHubbleImageAndUploadToBluesky" Y "callPostToBlueskyWithImage"
    //  HAN SIDO ELIMINADAS. SU LÓGICA AHORA VIVE DENTRO DE HubblePostWorker.kt
    // =========================================================================================


    /**
     * Esta función se mantiene porque el botón "Publicar Texto" todavía la usa.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostTextToBlueskyUseCase(
        text: String
    ) {
        lifecycleScope.launch {
            Log.d("MainActivity", "Llamando a PostToBlueskyUseCase (solo texto)...")
            val result = postToBlueskyUseCase(
                text = text,
                imageId = "text-post-${System.currentTimeMillis()}",
                imageBlob = null,
                imageAltText = null,
                facets = null
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