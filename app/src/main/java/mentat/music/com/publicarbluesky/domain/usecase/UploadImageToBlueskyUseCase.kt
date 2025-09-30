package mentat.music.com.publicarbluesky.domain.usecase

import android.util.Log
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mentat.music.com.publicarbluesky.constans.Constants // Asegúrate que esta importación sea correcta
import mentat.music.com.publicarbluesky.data.bluesky.model.BlobObject
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionInput
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private const val TAG = "UploadImageUseCase"

class UploadImageToBlueskyUseCase(
    private val blueskyApi: BlueskyApi
    // Ya no necesitamos sessionManager aquí, pasaremos identifier y password directamente
    // o los obtendremos de Constants como en PostToBlueskyUseCase
) {

    suspend operator fun invoke(
        imageFile: File,
        mimeType: String,
        identifier: String, // Para el login
        appPassword: String    // Para el login
    ): Result<BlobObject> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando subida de imagen: ${imageFile.name}, MimeType: $mimeType para usuario: $identifier")

        // 1. Crear una nueva sesión para obtener el token de acceso JWT
        val authorizationHeader: String
        val sessionInput = CreateSessionInput(identifier, appPassword)
        try {
            Log.d(TAG, "Creando sesión para subir imagen...")
            val sessionResponse = blueskyApi.createSession(sessionInput)
            if (!sessionResponse.isSuccessful || sessionResponse.body() == null) {
                val errorBody = sessionResponse.errorBody()?.string() ?: "Sin cuerpo de error"
                val errorMsg = "Error al crear sesión para subir imagen: ${sessionResponse.code()} ${sessionResponse.message()}. ErrorBody: $errorBody"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val accessToken = sessionResponse.body()!!.accessJwt
            if (accessToken.isBlank()) {
                val errorMsg = "Token de acceso JWT vacío después de crear sesión."
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }
            authorizationHeader = "Bearer $accessToken"
            Log.i(TAG, "Sesión creada y token de acceso obtenido para subir blob.")
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al crear sesión para subir imagen: ${e.message}", e)
            return@withContext Result.failure(e)
        }

        // 2. Crear el RequestBody para la imagen
        val mediaType = mimeType.toMediaTypeOrNull()
        if (mediaType == null) {
            val errorMsg = "MimeType inválido: $mimeType"
            Log.e(TAG, errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }
        val requestFile = imageFile.asRequestBody(mediaType)
        Log.d(TAG, "RequestBody creado para ${imageFile.name}. Tamaño: ${imageFile.length()} bytes.")

        // 3. Llamar a la API para subir el blob
        try {
            Log.d(TAG, "Llamando a blueskyApi.uploadBlob con token: ${authorizationHeader.substring(0, 15)}...") // No loguear el token completo
            val response = blueskyApi.uploadBlob(authorizationHeader, requestFile)

            if (response.isSuccessful && response.body() != null) {
                val blobObject = response.body()!!.blob
                Log.i(TAG, "Imagen subida exitosamente. CID: ${blobObject.ref.cid}, Tipo: ${blobObject.type}, MimeType: ${blobObject.mimeType}, Tamaño: ${blobObject.size}")
                Result.success(blobObject)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Sin cuerpo de error"
                val errorMsg = "Error al subir la imagen. Código: ${response.code()}. Mensaje: ${response.message()}. ErrorBody: $errorBody"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción durante la subida de la imagen: ${e.message}", e)
            Result.failure(e)
        }
    }
}
