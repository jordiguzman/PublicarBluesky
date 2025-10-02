package mentat.music.com.publicarbluesky

import android.app.Application
import androidx.work.Configuration
import mentat.music.com.publicarbluesky.di.AppContainer
import mentat.music.com.publicarbluesky.work.CustomWorkerFactory

class BlueskyPublisherApp : Application(), Configuration.Provider {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Creamos el contenedor de dependencias cuando se inicia la app
        appContainer = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                CustomWorkerFactory(
                    appContainer.getFreshHubbleImageUseCase,
                    appContainer.uploadImageToBlueskyUseCase,
                    appContainer.postToBlueskyUseCase
                )
            )
            .build()
}