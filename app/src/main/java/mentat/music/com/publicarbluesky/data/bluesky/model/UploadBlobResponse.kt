import com.google.gson.annotations.SerializedName

// Respuesta esperada de com.atproto.repo.uploadBlob
data class UploadBlobResponse(
    // Algunas APIs pueden anidar el blob, otras no. Ajusta según la respuesta real.
    // Opción 1: Blob está anidado
    // val blob: BlobData

    // Opción 2: Campos directamente en la raíz (más común en ejemplos recientes de ATProto)
    @SerializedName("\$type") val type: String? = null, // ATProto a menudo usa $type, puede ser "blob" o no estar
    val cid: String,
    val mimeType: String,
    val size: Long
)


