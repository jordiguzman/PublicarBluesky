package mentat.music.com.publicarbluesky

import android.Manifest // <-- Importante para el permiso
import android.content.pm.PackageManager // <-- Importante para el permiso
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts // <-- Importante para el permiso
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat // <-- Importante para el permiso
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints // <-- Importante para el botón
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType // <-- Importante para el botón
import androidx.work.OneTimeWorkRequest // <-- Importante para el botón
import androidx.work.WorkManager // <-- Importante para el botón
import kotlinx.coroutines.launch
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.ui.features.post.MainScreen
// ¡Asegúrate de que este import es correcto!
import mentat.music.com.publicarbluesky.work.HubblePostWorker

class MainActivity : ComponentActivity() {

    private lateinit var postToBlueskyUseCase: PostToBlueskyUseCase

    // Lanzador para pedir el permiso de notificación
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("MainActivity", "Permiso de notificaciones CONCEDIDO.")
        } else {
            Log.w("MainActivity", "Permiso de notificaciones DENEGADO.")
            Toast.makeText(this, "No se podrán mostrar notificaciones.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el AppContainer de MyApplication
        val appContainer = (application as MyApplication).appContainer
        postToBlueskyUseCase = appContainer.postToBlueskyUseCase

        // Pedir permiso de notificación al arrancar
        checkAndRequestNotificationPermission()

        setContent {
            Scaffold { innerPadding ->
                MainScreen(
                    modifier = Modifier.padding(innerPadding),

                    // Este botón de texto se queda igual
                    onPostTextClick = { textToPost ->
                        callPostTextToBlueskyUseCase(text = textToPost)
                    },


                    // --- ¡¡ESTA ES LA LÓGICA NUEVA DEL BOTÓN!! ---
                    onFetchAndProcessHubbleImageClick = {
                        Log.d("MainActivity", "Botón de publicación manual presionado.")
                        Toast.makeText(this@MainActivity, "Iniciando publicación manual...", Toast.LENGTH_SHORT).show()

                        // 1. Crear las restricciones
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()

                        // 2. Crear una petición de UNA SOLA VEZ
                        val manualWorkRequest = OneTimeWorkRequest.Builder(HubblePostWorker::class.java)
                            .setConstraints(constraints)
                            .addTag("manual_job") // <-- ¡¡ESTA ES LA LÍNEA NUEVA!!
                            .build()

                        // 3. ¡Encolar la tarea AHORA!
                        // Usamos un nombre único y 'REPLACE'
                        // para que si pulsas 5 veces, cancele las 4 anteriores
                        // y solo se ejecute la última.
                        WorkManager.getInstance(this@MainActivity).enqueueUniqueWork(
                            "manual-post-job", // <-- ¡¡ESTO ES NUEVO!! (nombre único)
                            ExistingWorkPolicy.REPLACE, // <-- ¡¡ESTO ES NUEVO!! (REPLACE)
                            manualWorkRequest
                        )
                    }
                    // --- FIN DE LA LÓGICA NUEVA ---

                )
            }
        }
    }

    // Función para el botón de "Publicar Texto"
    @RequiresApi(Build.VERSION_CODES.O)
    private fun callPostTextToBlueskyUseCase(text: String) {
        lifecycleScope.launch {
            val result = postToBlueskyUseCase(
                text = text
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

    // Nueva función para comprobar y pedir el permiso de notificación
    private fun checkAndRequestNotificationPermission() {
        // Solo es necesario en Android 13 (API 33) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Comprobar si el permiso YA está concedido
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "El permiso de notificación ya estaba concedido.")
                }

                // Pedir el permiso
                else -> {
                    Log.d("MainActivity", "Pidiendo permiso de notificación...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}