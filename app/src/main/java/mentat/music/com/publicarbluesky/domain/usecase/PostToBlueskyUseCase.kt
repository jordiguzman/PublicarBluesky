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
import mentat.music.com.publicarbluesky.domain.repository.PublishedImageIdRepository
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostToBlueskyUseCase(
    private val blueskyApi: BlueskyApi,
    private val sessionManager: SessionManager,
    private val publishedImageIdRepository: PublishedImageIdRepository
) {
    suspend operator fun invoke(
        text: String, imageId: String? = null,
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

            val responseWrapper: Response<CreateRecordOutput> = blueskyApi.createRecord(
                authorization = "Bearer ${session.accessJwt}",
                input = createRecordInput
            )

            if (responseWrapper.isSuccessful) {
                val createRecordOutput = responseWrapper.body()
                if (createRecordOutput != null) {
                    // --- INICIO DE LA CORRECCIÓN ---
                    // Solo guardamos el ID si no es nulo (es decir, si es un post con imagen)
                    if (imageId != null) {
                        publishedImageIdRepository.addPublishedId(imageId)
                        Log.i(
                            "PostToBlueskyUseCase",
                            "Publicación exitosa. ID '$imageId' guardado en SharedPreferences."
                        )
                    }
                    // --- FIN DE LA CORRECCIÓN ---

                    Result.success(createRecordOutput.uri)
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

// Implementación completa de InMemorySessionManager
class InMemorySessionManager(
    private val blueskyApi: BlueskyApi
) : SessionManager {
    private var currentSession: CreateSessionOutput? = null
    private var usernameCache: String? = null
    private var appPasswordCache: String? = null

    fun setCredentials(username: String, appPass: String) {
        usernameCache = username
        appPasswordCache = appPass
        currentSession =
            null // Invalida la sesión actual para forzar un nuevo login si es necesario
    }

    override suspend fun getValidSession(): CreateSessionOutput? {
        val session = currentSession
        // Primero, verifica si la sesión actual existe y si su token es (teóricamente) válido
        if (session != null && isTokenValid(session.accessJwt, session.did)) {
            Log.d("InMemorySessionManager", "Usando sesión en memoria existente.")
            return session
        }

        // Si la sesión no es válida pero tenemos un refresh token, intentamos refrescar
        if (session?.refreshJwt != null) {
            try {
                Log.d("InMemorySessionManager", "Intentando refrescar la sesión...")
                val refreshResponseWrapper =
                    blueskyApi.refreshSession("Bearer ${session.refreshJwt}")

                if (refreshResponseWrapper.isSuccessful) {
                    val refreshedSessionData = refreshResponseWrapper.body()
                    if (refreshedSessionData != null) {
                        // Asignamos los nuevos datos de la sesión refrescada
                        currentSession = CreateSessionOutput(
                            did = refreshedSessionData.did,
                            handle = refreshedSessionData.handle,
                            email = refreshedSessionData.email, // Puede ser null en la respuesta de refresh
                            accessJwt = refreshedSessionData.accessJwt,
                            refreshJwt = refreshedSessionData.refreshJwt
                        )
                        Log.i(
                            "InMemorySessionManager",
                            "Sesión refrescada exitosamente para ${currentSession?.handle}"
                        )
                        // En una app real, guardarías esta nueva sesión en SharedPreferences
                        return currentSession
                    }
                } else {
                    Log.w(
                        "InMemorySessionManager",
                        "Falló el refresh de sesión: ${refreshResponseWrapper.code()} - ${
                            refreshResponseWrapper.errorBody()?.string()
                        }"
                    )
                }
            } catch (e: Exception) {
                Log.e("InMemorySessionManager", "Excepción durante el refresh de sesión", e)
            }
        }

        // Si no hay sesión, no pudimos refrescar, o falló el refresh, intentamos crear una nueva sesión desde cero
        currentSession = null // Limpiar cualquier resto de sesión inválida

        val user = usernameCache
        val pass = appPasswordCache
        if (user != null && pass != null) {
            try {
                Log.d("InMemorySessionManager", "No hay sesión válida, creando una nueva...")
                val createSessionResponseWrapper =
                    blueskyApi.createSession(CreateSessionInput(user, pass))

                if (createSessionResponseWrapper.isSuccessful) {
                    currentSession = createSessionResponseWrapper.body()
                    if (currentSession != null) {
                        Log.i(
                            "InMemorySessionManager",
                            "Nueva sesión creada exitosamente para ${currentSession?.handle}"
                        )
                        // En una app real, guardarías esta nueva sesión en SharedPreferences
                        return currentSession
                    }
                } else {
                    Log.e(
                        "InMemorySessionManager",
                        "Falló la creación de sesión: ${createSessionResponseWrapper.code()} - ${
                            createSessionResponseWrapper.errorBody()?.string()
                        }"
                    )
                    return null
                }
            } catch (e: Exception) {
                Log.e("InMemorySessionManager", "Excepción al crear sesión", e)
                return null
            }
        }

        Log.w("InMemorySessionManager", "No hay sesión ni credenciales en caché para obtener una.")
        return null
    }

    override fun clearSession() {
        currentSession = null
        usernameCache = null
        appPasswordCache = null
        Log.d("InMemorySessionManager", "Sesión y credenciales en memoria borradas.")
        // En una app real, también borrarías la sesión de SharedPreferences
    }

    private fun isTokenValid(jwt: String, didContext: String): Boolean {
        // --- ADVERTENCIA: IMPLEMENTACIÓN SIMPLIFICADA ---
        // Una implementación real debería:
        // 1. Decodificar el JWT (usando una librería como com.auth0.jwt:java-jwt).
        // 2. Verificar la firma del token (aunque para un cliente es menos crucial que para un servidor).
        // 3. Verificar la fecha de expiración ('exp'). Si ha pasado, el token no es válido.
        // 4. Verificar el 'audience' ('aud'), que debería ser el DID del usuario.
        if (jwt.isBlank()) return false

        // Como no estamos decodificando, simplemente asumimos que el token es válido si existe.
        // Esto NO es seguro para producción, pero funciona para la lógica de la app.
        Log.w(
            "InMemorySessionManager",
            "isTokenValid para $didContext NO está implementado completamente, asumiendo token como válido si existe."
        )
        return true
    }
}

