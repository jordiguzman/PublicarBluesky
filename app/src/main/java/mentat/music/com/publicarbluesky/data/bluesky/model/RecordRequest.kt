package mentat.music.com.publicarbluesky.data.bluesky.model

data class RecordRequest(
    val repo: String,                   // DID del usuario, ej: response.did
    val collection: String,             // NSID de la colección, ej: "app.bsky.feed.post"
    val record: PostRecord              // El contenido del post (definido en EmbedModels.kt)
    // val rkey: String? = null,        // Clave de registro (TID), auto-generada si es null
    // val swapCommit: String? = null,  // Para operaciones avanzadas de reemplazo
    // val validate: Boolean? = true    // Si el PDS debe validar el record antes de aceptar
)