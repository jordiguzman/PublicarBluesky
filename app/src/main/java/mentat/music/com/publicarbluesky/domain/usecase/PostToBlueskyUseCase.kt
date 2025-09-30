package mentat.music.com.publicarbluesky.domain.usecase

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mentat.music.com.publicarbluesky.data.bluesky.model.BlobObject
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateRecordInput
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionInput
import mentat.music.com.publicarbluesky.data.bluesky.model.EmbedData
import mentat.music.com.publicarbluesky.data.bluesky.model.ImageEmbedData
import mentat.music.com.publicarbluesky.data.bluesky.model.PostRecordData
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG_POST_UC = "PostToBlueskyUseCase"

class PostToBlueskyUseCase(
    private val blueskyApi: BlueskyApi
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(
        identifier: String,
        appPassword: String,
        text: String,
        imageBlob: BlobObject? = null, // <--- NUEVO: BlobObject opcional para la imagen
        imageAltText: String? = null   // <--- NUEVO: Texto alternativo opcional para la imagen
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validar que si hay imagen, haya texto alternativo
            if (imageBlob != null && imageAltText.isNullOrBlank()) {
                Log.e(TAG_POST_UC, "Se proporcionó una imagen pero no un texto alternativo.")
                return@withContext Result.failure(Exception("⚠️ Imagen sin texto alternativo."))
            }

            // 1️⃣ Crear Sesión (Login)
            Log.d(TAG_POST_UC, "Intentando crear sesión para usuario: $identifier")
            val sessionInput = CreateSessionInput(identifier = identifier, password = appPassword)
            val sessionResponseWrapper = blueskyApi.createSession(sessionInput)

            if (!sessionResponseWrapper.isSuccessful || sessionResponseWrapper.body() == null) {
                val errorBody = sessionResponseWrapper.errorBody()?.string()
                    ?: "Respuesta de sesión nula o fallida"
                Log.e(
                    TAG_POST_UC,
                    "Creación de sesión fallida: ${sessionResponseWrapper.code()} - $errorBody"
                )
                return@withContext Result.failure(Exception("❌ Creación de sesión fallida: ${sessionResponseWrapper.code()} - $errorBody"))
            }

            val sessionData = sessionResponseWrapper.body()!!
            if (sessionData.accessJwt.isBlank() || sessionData.did.isBlank()) {
                Log.e(
                    TAG_POST_UC,
                    "Creación de sesión fallida: JWT o DID faltante en la respuesta."
                )
                return@withContext Result.failure(Exception("❌ Creación de sesión fallida: JWT o DID faltante."))
            }

            val token = "Bearer ${sessionData.accessJwt}"
            val userDid = sessionData.did
            Log.i(
                TAG_POST_UC,
                "Sesión creada exitosamente. DID: $userDid. Token: ${token.take(15)}..."
            )

            // 2️⃣ Preparar el Embed de la imagen (si existe)
            val embed: EmbedData? = imageBlob?.let { blob ->
                imageAltText?.let { altText ->
                    Log.d(
                        TAG_POST_UC,
                        "Preparando embed para imagen. CID: ${blob.ref.cid}, Alt: '$altText'"
                    )
                    EmbedData(
                        // type = "app.bsky.embed.images" // Ya está por defecto en EmbedData
                        images = listOf(
                            ImageEmbedData(
                                image = blob, // El BlobObject completo devuelto por uploadBlob
                                alt = altText
                            )
                        )
                    )
                }
            }

            // 3️⃣ Crear el contenido del post (el 'record')
            // Formateador para ISO 8601 con milisegundos y 'Z' para UTC
            val isoFormatter = DateTimeFormatter.ISO_INSTANT
            val createdAtIso = Instant.now().atZone(ZoneOffset.UTC).format(isoFormatter)
            Log.d(TAG_POST_UC, "Generado createdAt: $createdAtIso")


            val postRecordContent = PostRecordData(
                // type = "app.bsky.feed.post" // Ya está por defecto en PostRecordData
                text = text,
                createdAt = createdAtIso,
                langs = listOf("es"), // Opcional, o configurable
                embed = embed // <--- AÑADIR EL EMBED CREADO
            )
            Log.d(TAG_POST_UC, "Contenido de PostRecordData creado: $postRecordContent")

            // 4️⃣ Crear la petición para el nuevo registro
            val createRecordPayload = CreateRecordInput(
                repo = userDid,
                // collection = "app.bsky.feed.post" // Ya está por defecto en CreateRecordInput
                record = postRecordContent
            )
            Log.d(
                TAG_POST_UC,
                "Payload completo de CreateRecordInput a enviar: $createRecordPayload"
            )


            // 5️⃣ Publicar el registro
            Log.d(TAG_POST_UC, "Intentando crear el registro (post)...")
            val createRecordResponseWrapper = blueskyApi.createRecord(token, createRecordPayload)

            if (createRecordResponseWrapper.isSuccessful && createRecordResponseWrapper.body() != null) {
                val recordUri = createRecordResponseWrapper.body()!!.uri
                val postType = if (imageBlob != null) "post con imagen" else "post de texto"
                Log.i(TAG_POST_UC, "Publicado ($postType): $recordUri")
                Result.success("✅ Publicado ($postType): $recordUri")
            } else {
                val errorBody = createRecordResponseWrapper.errorBody()?.string()
                    ?: "Respuesta de crear record nula o fallida"
                Log.e(
                    TAG_POST_UC,
                    "Error al publicar: ${createRecordResponseWrapper.code()} - $errorBody"
                )
                Result.failure(Exception("❌ Error al publicar: ${createRecordResponseWrapper.code()} - $errorBody"))
            }

        } catch (e: Exception) {
            Log.e(TAG_POST_UC, "Error en la operación de post: ${e.message}", e)
            Result.failure(Exception("⚠️ Error en la operación de post: ${e.message ?: "Error desconocido"}"))
        }
    }
}