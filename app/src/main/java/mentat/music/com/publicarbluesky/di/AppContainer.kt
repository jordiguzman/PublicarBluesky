package mentat.music.com.publicarbluesky.di

import android.content.Context
import mentat.music.com.publicarbluesky.constans.Constants
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import mentat.music.com.publicarbluesky.data.bluesky.remote.client.BlueskyClient
import mentat.music.com.publicarbluesky.domain.repository.PublishedImageIdRepository
import mentat.music.com.publicarbluesky.domain.usecase.GetFreshHubbleImageUseCase
import mentat.music.com.publicarbluesky.domain.usecase.InMemorySessionManager
import mentat.music.com.publicarbluesky.domain.usecase.PostToBlueskyUseCase
import mentat.music.com.publicarbluesky.domain.usecase.SessionManager
import mentat.music.com.publicarbluesky.domain.usecase.UploadImageToBlueskyUseCase
import okhttp3.OkHttpClient

// Esta clase contendrá TODAS tus dependencias
class AppContainer(private val context: Context) {

    // --- Dependencias de Red ---
    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }
    private val blueskyApi: BlueskyApi by lazy { BlueskyClient.api }

    // --- Repositorios ---
    val publishedImageIdRepository: PublishedImageIdRepository by lazy {
        PublishedImageIdRepository(context)
    }

    // --- Sesión ---
    val sessionManager: SessionManager by lazy {
        val manager = InMemorySessionManager(blueskyApi)
        manager.setCredentials(
            Constants.BLUESKY_USERNAME,
            Constants.BLUESKY_APP_PASSWORD
        )
        manager
    }

    // --- Casos de Uso (UseCases) ---
    val getFreshHubbleImageUseCase: GetFreshHubbleImageUseCase by lazy {
        GetFreshHubbleImageUseCase(
            context = context,
            okHttpClient = okHttpClient,
            publishedImageIdRepository = publishedImageIdRepository
        )
    }

    val uploadImageToBlueskyUseCase: UploadImageToBlueskyUseCase by lazy {
        UploadImageToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager
        )
    }

    val postToBlueskyUseCase: PostToBlueskyUseCase by lazy {
        PostToBlueskyUseCase(
            blueskyApi = blueskyApi,
            sessionManager = sessionManager,
            publishedImageIdRepository = publishedImageIdRepository
        )
    }
}