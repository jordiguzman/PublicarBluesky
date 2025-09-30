package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

// --- Modelos para el Embed de Imágenes en un Post ---
// Representa el objeto "embed" cuando son imágenes
data class ImageEmbed(
    @SerializedName("\$type") val type: String = "app.bsky.embed.images",
    val images: List<ImageInstance>
)

// Representa una instancia individual de imagen dentro del embed
data class ImageInstance(
    val image: BlobLink,        // Referencia al blob de la imagen subida
    val alt: String             // Texto alternativo para accesibilidad ¡MUY IMPORTANTE!
    // aspectRatio: AspectRatio? // Opcional, para ayudar al renderizado
)

// Referencia al blob (imagen) subido
data class BlobLink(
    // El tipo "$type": "blob" es a menudo implícito por el contexto y no siempre necesario aquí,
    // pero a veces las APIs de ATProto lo requieren. Revisa la especificación del endpoint.
    // @SerializedName("\$type") val type: String = "blob", // Descomentar si es necesario
    @SerializedName("ref") val link: CidLink, // La clave JSON real es "ref" que contiene un "$link"
    val mimeType: String,
    val size: Long
)

// Contenedor para el CID del blob, usando la clave "$link" como en ATProto
data class CidLink(
    @SerializedName("\$link") val cid: String // La clave JSON real para el CID
)

// (Opcional) Para definir la relación de aspecto si la API lo soporta/requiere
// data class AspectRatio(
//    val width: Int,
//    val height: Int
// )
// --- Modelo de Record para un Post que puede incluir un embed ---
// Esto haría tu RecordRequest.record más tipado que Map<String, Any>
data class PostRecord(
    @SerializedName("\$type") val type: String = "app.bsky.feed.post",
    val text: String,
    val createdAt: String, // Formato ISO 8601: "2023-12-08T10:30:00.123Z"
    val langs: List<String>? = null, // Opcional, ej: ["en", "es"]
    val embed: ImageEmbed? = null // Hacemos el embed opcional para soportar posts solo de texto también
    // Aquí podrías añadir otros campos como facets (para menciones, links), tags, etc.
)