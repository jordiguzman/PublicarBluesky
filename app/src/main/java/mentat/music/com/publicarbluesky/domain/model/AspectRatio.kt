package mentat.music.com.publicarbluesky.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Modelo de datos para el 'aspectRatio' que se adjunta a las imágenes
 * en un post de Bluesky (app.bsky.embed.images).
 */
@JsonClass(generateAdapter = true)
data class AspectRatio(
    @Json(name = "width")
    val width: Int,

    @Json(name = "height")
    val height: Int
)