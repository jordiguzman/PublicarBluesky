package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

/**
 * Representa un facet, que aplica características a un segmento de texto.
 */
data class Facet(
    @SerializedName("index")
    val index: FacetIndex,

    @SerializedName("features")
    val features: List<FacetFeature>
)

/**
 * Define el rango de bytes (UTF-8) en el texto al que se aplica el facet.
 */
data class FacetIndex(
    @SerializedName("byteStart")
    val byteStart: Int,

    @SerializedName("byteEnd")
    val byteEnd: Int
)

/**
 * Interfaz base para los diferentes tipos de características de un facet.
 */
interface FacetFeature {
    // La propiedad 'type' será implementada y anotada en las clases concretas.
    val type: String
}

/**
 * Característica de facet para un enlace/URL.
 */
data class FacetLinkFeature(
    @SerializedName("uri")
    val uri: String, // La URL completa a la que enlaza.

    @SerializedName("\$type") // Asegura que Gson use "$type" como nombre del campo en JSON
    override val type: String = "app.bsky.richtext.facet#link"
) : FacetFeature

/**
 * Característica de facet para un hashtag/tag.
 */
data class FacetTagFeature(
    @SerializedName("tag")
    val tag: String, // El nombre del tag SIN el símbolo '#'.

    @SerializedName("\$type") // Asegura que Gson use "$type" como nombre del campo en JSON
    override val type: String = "app.bsky.richtext.facet#tag"
) : FacetFeature

/**
 * Característica de facet para una mención.
 * (Añadida para completitud, podrías necesitarla más adelante)
 */
data class FacetMentionFeature(
    @SerializedName("did")
    val did: String, // El DID del usuario mencionado.

    @SerializedName("\$type") // Asegura que Gson use "$type" como nombre del campo en JSON
    override val type: String = "app.bsky.richtext.facet#mention"
) : FacetFeature