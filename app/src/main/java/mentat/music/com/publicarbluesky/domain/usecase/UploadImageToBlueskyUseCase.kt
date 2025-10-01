package mentat.music.com.publicarbluesky.domain.usecase

import android.util.Log
import mentat.music.com.publicarbluesky.data.bluesky.model.BlobObject
import mentat.music.com.publicarbluesky.data.bluesky.model.UploadBlobOutput
import mentat.music.com.publicarbluesky.data.bluesky.remote.api.BlueskyApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

// Ya no necesitas CreateSessionInput ni CreateSessionOutput aquí si SessionManager lo maneja
class UploadImageToBlueskyUseCase(
    private val blueskyApi: BlueskyApi,
    private val sessionManager: SessionManager // <--- AÑADIDO SessionManager AL CONSTRUCTOR
) {
    suspend operator fun invoke(
        imageFile: File,
        mimeType: String
        // Ya NO se necesitan identifier ni appPassword aquí
    ): Result<BlobObject> {
        return try {
            // 1. Obtener sesión válida (y por ende el token) del SessionManager
            val session = sessionManager.getValidSession()
                ?: return Result.failure(Exception("No se pudo obtener una sesión válida para subir la imagen."))

            val accessToken = session.accessJwt // Obtenemos el token de la sesión válida

            Log.d(
                "UploadImageUseCase",
                "Token obtenido para subir imagen. Procediendo con la subida..."
            )

            // 2. Preparar RequestBody
            val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())

            // 3. Hacer la llamada a uploadBlob
            // Asegúrate de que tu interfaz BlueskyApi.uploadBlob espera estos parámetros
            val responseWrapper: Response<UploadBlobOutput> = blueskyApi.uploadBlob(
                authorization = "Bearer $accessToken",
                contentType = mimeType, // Si tu API espera este header
                imageBytes = requestFile
            )

            if (responseWrapper.isSuccessful) {
                val uploadBlobOutput = responseWrapper.body()
                if (uploadBlobOutput != null && uploadBlobOutput.blob != null) { // Verificar también que .blob no sea nulo
                    Log.i(
                        "UploadImageUseCase",
                        "Imagen subida exitosamente. CID: ${uploadBlobOutput.blob.ref.cid}"
                    )
                    Result.success(uploadBlobOutput.blob)
                } else {
                    Log.w(
                        "UploadImageUseCase",
                        "Respuesta exitosa de uploadBlob pero cuerpo o blob interno vacío."
                    )
                    Result.failure(Exception("Respuesta exitosa de uploadBlob pero cuerpo o blob interno vacío."))
                }
            } else {
                val errorBody =
                    responseWrapper.errorBody()?.string() ?: "Sin cuerpo de error detallado"
                Log.e(
                    "UploadImageUseCase",
                    "Error de API al subir imagen: ${responseWrapper.code()} - $errorBody"
                )
                Result.failure(Exception("Error de API (${responseWrapper.code()}) al subir imagen: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("UploadImageUseCase", "Excepción al subir imagen: ${e.message}", e)
            Result.failure(Exception("Excepción al subir imagen: ${e.localizedMessage}", e))
        }
    }
}