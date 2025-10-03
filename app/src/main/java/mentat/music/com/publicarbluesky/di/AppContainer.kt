/*package mentat.music.com.publicarbluesky.di


import android.content.Context
import mentat.music.com.publicarbluesky.data.bluesky.remote.client.BlueskyClient
import mentat.music.com.publicarbluesky.data.BlueskyRepositoryImpl
import mentat.music.com.publicarbluesky.data.HubbleRepositoryImpl
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import mentat.music.com.publicarbluesky.hubble.HubbleClient

/**
 * Contenedor de dependencias simple para gestionar las instancias de los UseCases y Repositorios.
 * Esto asegura que solo tenemos una instancia de cada uno en toda la app.
 */
class AppContainer(context: Context) {

    // --- Clientes de Red ---
    private val hubbleClient = HubbleClient.instance
    private val blueskyClient = BlueskyClient.instance

    // --- Repositorios ---
    private val hubbleRepository = HubbleRepositoryImpl(hubbleClient)
    private val blueskyRepository = BlueskyRepositoryImpl(blueskyClient)

    // --- Casos de Uso (UseCases) ---
    // Estas son las instancias que usará el resto de la app.
    val getFreshHubbleImageUseCase = GetFreshHubbleImageUseCase(hubbleRepository)
    val uploadImageToBlueskyUseCase = UploadImageToBlueskyUseCase(blueskyRepository)
    val postToBlueskyUseCase = PostToBlueskyUseCase(blueskyRepository)
}*/