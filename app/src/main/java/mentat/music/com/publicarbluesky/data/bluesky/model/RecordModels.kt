package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

// --- INPUT para com.atproto.repo.createRecord ---

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
 */
data class PostRecordData(
    @SerializedName("\$type")        val type: String = "app.bsky.feed.post", // Tipo de objeto

    @SerializedName("text")
    val text: String,

    @SerializedName("createdAt")
    val createdAt: String, // Fecha en formato ISO 8601 (ej: "2023-01-15T10:00:00.000Z")

    @SerializedName("langs")
    val langs: List<String>? = null, // Opcional: Lista de códigos de idioma (ej: ["en", "es"])

    @SerializedName("embed")
    val embed: EmbedData? = null // Opcional: Para adjuntar imágenes, enlaces, etc.
)

/**
 * Representa el objeto "embed" para adjuntar contenido.
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
    val image: BlobObject, // REUTILIZA BlobObject. ASEGÚRATE QUE BlobModels.kt Y BlobObject ESTÉN BIEN DEFINIDOS.

    @SerializedName("alt")
    val alt: String // Texto alternativo para la imagen (IMPORTANTE para accesibilidad)
)


// --- OUTPUT para com.atproto.repo.createRecord ---

/**
 * Datos de salida del endpoint com.atproto.repo.createRecord.
 */
data class CreateRecordOutput(
    @SerializedName("uri")
    val uri: String, // El AT URI del post creado

    @SerializedName("cid")
    val cid: String  // El CID del registro del post
)
