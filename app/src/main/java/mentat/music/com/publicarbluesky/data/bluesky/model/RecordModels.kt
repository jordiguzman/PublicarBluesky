package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName
import mentat.music.com.publicarbluesky.data.bluesky.model.Facet

/**
 * Datos de entrada para el endpoint com.atproto.repo.createRecord.
 */
data class CreateRecordInput(
    @SerializedName("repo")
    val repo: String, // El DID del usuario (ej: obtenido de CreateSessionOutput.did)

    @SerializedName("collection")
    val collection: String = "app.bsky.feed.post", // Por defecto para posts de feed

    @SerializedName("record")
    val record: PostRecordData
)

/**
 * Representa el contenido del post ("record") que se va a crear.
 * Esto corresponde a un registro de tipo app.bsky.feed.post.
 */
data class PostRecordData(
    @SerializedName("\$type")
    val type: String = "app.bsky.feed.post", // Tipo de objeto

    @SerializedName("text")
    val text: String,

    @SerializedName("createdAt")
    val createdAt: String, // Fecha en formato ISO 8601 (ej: "2023-01-15T10:00:00.000Z")

    @SerializedName("langs")
    val langs: List<String>? = null, // Opcional: Lista de códigos de idioma (ej: ["en", "es"])

    @SerializedName("embed")
    val embed: EmbedData? = null, // Opcional: Para adjuntar imágenes, enlaces, etc.

    @SerializedName("facets")
    val facets: List<Facet>? = null // <--- USA EL FACET IMPORTADO
)

/**
 * Representa el objeto "embed" para adjuntar contenido.
 * Actualmente solo soporta imágenes, pero podría extenderse.
 */
data class EmbedData(
    @SerializedName("\$type")
    val type: String = "app.bsky.embed.images", // Tipo de embed (para imágenes)

    @SerializedName("images")
    val images: List<ImageEmbedData>
)

/**
 * Representa una imagen individual dentro del embed.
 */
data class ImageEmbedData(
    @SerializedName("image")
    val image: BlobObject, // Asumo que BlobObject está definido en otro lugar y es importado correctamente.

    @SerializedName("alt")
    val alt: String // Texto alternativo para la imagen (IMPORTANTE para accesibilidad)
)

/**
 * Datos de salida del endpoint com.atproto.repo.createRecord.
 */
data class CreateRecordOutput(
    @SerializedName("uri")
    val uri: String, // El AT URI del post creado

    @SerializedName("cid")
    val cid: String  // El CID del registro del post
)