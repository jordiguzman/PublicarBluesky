package mentat.music.com.publicarbluesky.data.hubble.model

import com.google.gson.annotations.SerializedName
import mentat.music.com.publicarbluesky.constans.Constants // Asegúrate de importar tus Constantes
import org.jsoup.parser.Parser

data class HubbleSiteImage(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("src") val thumbnailUrl: String,
    @SerializedName("url") val relativePageUrl: String,
    @SerializedName("potw") val potw: Boolean? = null
) {
    val fullPageUrl: String
        get() = "https://esahubble.org$relativePageUrl" // Podrías crear otra constante para "https://esahubble.org" si se usa mucho

    val cleanedTitle: String
        get() = Parser.unescapeEntities(title, true)


    val screenResolutionImageUrl: String
        get() = "${Constants.BASE_URL_IMAGE}${id}.jpg"
}