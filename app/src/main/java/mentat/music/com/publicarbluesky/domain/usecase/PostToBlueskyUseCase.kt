package mentat.music.com.publicarbluesky.domain.usecase

import android.util.Log
import mentat.music.com.publicarbluesky.data.bluesky.model.BlobObject
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateRecordInput
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateRecordOutput
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionInput
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionOutput
import mentat.music.com.publicarbluesky.data.bluesky.model.EmbedData
import mentat.music.com.publicarbluesky.data.bluesky.model.Facet
import mentat.music.com.publicarbluesky.data.bluesky.model.ImageEmbedData
import mentat.music.com.publicarbluesky.data.bluesky.model.PostRecordData
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostToBlueskyUseCase(
    private val blueskyApi: BlueskyApi,
    private val sessionManager: SessionManager // Asumiendo que tienes un SessionManager
) {

    suspend operator fun invoke(
        text: String,
        imageBlob: BlobObject? = null,
        imageAltText: String? = null,
        facets: List<Facet>? = null,
        langs: List<String> = listOf("es")
    ): Result<String> {
        return try {
            val session = sessionManager.getValidSession()
                ?: return Result.failure(Exception("No se pudo obtener una sesión válida de Bluesky."))

            val embed = imageBlob?.let { blob ->
                imageAltText?.let { alt ->
                    EmbedData(
                        type = "app.bsky.embed.images",
                        images = listOf(
                            ImageEmbedData(
                                image = blob,
                                alt = alt.take(1000)
                            )
                        )
                    )
                }
            }

            val record = PostRecordData(
                type = "app.bsky.feed.post",
                text = text,
                createdAt = getCurrentTimestampISO8601(),
                langs = langs.ifEmpty { null },
                embed = embed,
                facets = facets
            )

            val createRecordInput = CreateRecordInput(
                repo = session.did,
                collection = "app.bsky.feed.post",
                record = record
            )

            // Log.d("PostToBlueskyUseCase", "CreateRecordInput: ${Gson().toJson(createRecordInput)}") // Necesitarías Gson aquí

            // Aquí es donde usamos responseWrapper y manejamos Response<CreateRecordOutput>
            val responseWrapper: Response<CreateRecordOutput> = blueskyApi.createRecord(
                authorization = "Bearer ${session.accessJwt}",
                input = createRecordInput
            )

            if (responseWrapper.isSuccessful) {
                val createRecordOutput = responseWrapper.body() // Esto es CreateRecordOutput?
                if (createRecordOutput != null) {
                    Result.success(createRecordOutput.uri) // Accedemos a .uri desde createRecordOutput
                } else {
                    Log.w(
                        "PostToBlueskyUseCase",
                        "Respuesta exitosa de createRecord pero cuerpo vacío."
                    )
                    Result.failure(Exception("Respuesta exitosa de createRecord pero cuerpo vacío."))
                }
            } else {
                val errorBody =
                    responseWrapper.errorBody()?.string() ?: "Sin cuerpo de error detallado"
                Log.e(
                    "PostToBlueskyUseCase",
                    "Error de API al crear post: ${responseWrapper.code()} - $errorBody"
                )
                Result.failure(Exception("Error de API (${responseWrapper.code()}) al crear post: $errorBody"))
            }

        } catch (e: Exception) {
            Log.e("PostToBlueskyUseCase", "Excepción al crear el post: ${e.message}", e)
            Result.failure(
                Exception(
                    "Excepción al crear el post en Bluesky: ${e.localizedMessage}",
                    e
                )
            )
        }
    }

    private fun getCurrentTimestampISO8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

// Interfaz SessionManager (colócala donde corresponda, quizás en su propio archivo)
interface SessionManager {
    suspend fun getValidSession(): CreateSessionOutput?
    fun clearSession()
}

// Ejemplo de InMemorySessionManager (colócalo donde corresponda o usa tu implementación)
class InMemorySessionManager(
    private val blueskyApi: BlueskyApi
) : SessionManager {
    private var currentSession: CreateSessionOutput? = null
    private var usernameCache: String? = null
    private var appPasswordCache: String? = null

    fun setCredentials(username: String, appPass: String) {
        usernameCache = username
        appPasswordCache = appPass
        currentSession = null
    }

    override suspend fun getValidSession(): CreateSessionOutput? {
        val session = currentSession
        if (session != null && isTokenValid(session.accessJwt, session.did)) {
            return session
        }

        if (session?.refreshJwt != null) {
            try {
                // Asumiendo que refreshSession también devuelve Response<RefreshSessionOutput>
                // y que RefreshSessionOutput tiene los campos necesarios como CreateSessionOutput
                val refreshResponseWrapper =
                    blueskyApi.refreshSession("Bearer ${session.refreshJwt}")
                if (refreshResponseWrapper.isSuccessful) {
                    val refreshedSessionData = refreshResponseWrapper.body()
                    if (refreshedSessionData != null) {
                        // Mapea los campos de RefreshSessionOutput a CreateSessionOutput
                        // Esto es un ejemplo, ajusta según tu modelo RefreshSessionOutput
                        currentSession = CreateSessionOutput(
                            did = refreshedSessionData.did,
                            handle = refreshedSessionData.handle,
                            email = refreshedSessionData.email, // Puede no estar en refresh
                            accessJwt = refreshedSessionData.accessJwt,
                            refreshJwt = refreshedSessionData.refreshJwt
                        )
                        Log.d(
                            "InMemorySessionManager",
                            "Sesión refrescada exitosamente para ${currentSession?.handle}"
                        )
                        // Aquí deberías guardar la sesión actualizada (ej. en SharedPreferences)
                        return currentSession
                    }
                } else {
                    Log.w(
                        "InMemorySessionManager",
                        "Falló el refresh de sesión: ${refreshResponseWrapper.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("InMemorySessionManager", "Excepción durante el refresh de sesión", e)
                currentSession = null
            }
        }
        currentSession = null // Limpiar sesión inválida o si el refresh falló

        val user = usernameCache
        val pass = appPasswordCache
        if (user != null && pass != null) {
            try {
                // Asumiendo que createSession también devuelve Response<CreateSessionOutput>
                val createSessionResponseWrapper =
                    blueskyApi.createSession(CreateSessionInput(user, pass))
                if (createSessionResponseWrapper.isSuccessful) {
                    currentSession = createSessionResponseWrapper.body()
                    if (currentSession != null) {
                        Log.d(
                            "InMemorySessionManager",
                            "Nueva sesión creada para ${currentSession?.handle}"
                        )
                        // Aquí deberías guardar la sesión nueva (ej. en SharedPreferences)
                        return currentSession
                    }
                } else {
                    Log.e(
                        "InMemorySessionManager",
                        "Falló la creación de sesión: ${createSessionResponseWrapper.code()}"
                    )
                    return null
                }
            } catch (e: Exception) {
                Log.e("InMemorySessionManager", "Excepción al crear sesión", e)
                return null
            }
        }
        Log.w("InMemorySessionManager", "No hay credenciales para obtener una sesión.")
        return null
    }

    override fun clearSession() {
        currentSession = null
        usernameCache = null
        appPasswordCache = null
        Log.d("InMemorySessionManager", "Sesión borrada.")
        // Aquí deberías borrar la sesión guardada
    }

    private fun isTokenValid(jwt: String, didContext: String): Boolean {
        // IMPLEMENTACIÓN REAL REQUERIDA: Decodifica el JWT y verifica 'exp' y 'aud' (audience, que debe ser el DID).
        // Por ahora, solo loguea y devuelve true si no está vacío.
        if (jwt.isBlank()) return false
        Log.w(
            "InMemorySessionManager",
            "isTokenValid para $didContext NO está implementado completamente, asumiendo token como válido si existe."
        )
        return true
    }
}