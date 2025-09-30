package mentat.music.com.publicarbluesky.data.bluesky.remote.client

import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BlueskyClient {
    val api: BlueskyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://bsky.social/xrpc/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlueskyApi::class.java)
    }
}