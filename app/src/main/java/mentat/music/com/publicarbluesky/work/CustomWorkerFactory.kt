package mentat.music.com.publicarbluesky.work


import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase

class CustomWorkerFactory(
    private val getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase,
    private val uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase,
    private val postToBlueskyUseCase: PostToBlueskyUseCase
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {
            HubblePostWorker::class.java.name ->
                HubblePostWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    getFreshHubbleImageUseCase = getFreshHubbleImageUseCase,
                    uploadImageToBlueskyUseCase = uploadImageToBlueskyUseCase,
                    postToBlueskyUseCase = postToBlueskyUseCase
                )

            else -> {
                // Devuelve null para que WorkManager use su fábrica por defecto para otros workers.
                null
            }
        }
    }
}