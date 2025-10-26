package mentat.music.com.publicarbluesky.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import mentat.music.com.publicarbluesky.di.AppContainer // <-- Importante

class CustomWorkerFactory(
    private val container: AppContainer // <-- Recibe el AppContainer
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        // Comprueba qué worker le están pidiendo
        return when (workerClassName) {
            HubblePostWorker::class.java.name -> {
                // Si es el HubblePostWorker, lo crea pasándole
                // las dependencias desde el container
                HubblePostWorker(
                    appContext,
                    workerParameters,
                    container.getFreshHubbleImageUseCase, // <-- Inyección
                    container.uploadImageToBlueskyUseCase, // <-- Inyección
                    container.postToBlueskyUseCase         // <-- Inyección
                )
            }

            // Aquí podrías añadir más workers si tuvieras
            // case OtroWorker::class.java.name -> ...

            else -> {
                // No sabe cómo crear este worker
                null
            }
        }
    }
}