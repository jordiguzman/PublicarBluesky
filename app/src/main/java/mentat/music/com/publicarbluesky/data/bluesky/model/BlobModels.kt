package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

/**
 * Representa la respuesta del endpoint com.atproto.repo.uploadBlob.
 */
data class UploadBlobOutput(
    @SerializedName("blob")
    val blob: BlobObject
)

/**
 * Representa el objeto "blob" que contiene la información de la imagen subida.
 */
data class BlobObject(
    @SerializedName("\$type") // El tipo de objeto, ej: "blob" o "app.bsky.embed.images#image"
    val type: String? = "blob",

    @SerializedName("ref")
    val ref: Link, // El CID está dentro de este objeto Link

    @SerializedName("mimeType")
    val mimeType: String,

    @SerializedName("size")
    val size: Long
)

/**
 * Representa el objeto "$link" que contiene el CID. */
data class Link(
    @SerializedName("\$link")
    val cid: String // Este es el Content Identifier (CID) que necesitamos
)