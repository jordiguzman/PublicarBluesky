package mentat.music.com.publicarbluesky.data.bluesky.remote.api // Asegúrate que este es el paquete correcto



// Tus otras importaciones...
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionInput // <--- IMPORTANTE
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateSessionOutput // <--- IMPORTANTE
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateRecordInput
import mentat.music.com.publicarbluesky.data.bluesky.model.CreateRecordOutput
import mentat.music.com.publicarbluesky.data.bluesky.model.UploadBlobOutput
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface BlueskyApi {

    // --- ASEGÚRATE DE QUE ESTE MÉTODO ESTÉ ASÍ ---
    @POST("com.atproto.server.createSession")
    suspend fun createSession(@Body input: CreateSessionInput): Response<CreateSessionOutput>
    // --- FIN DE LA SECCIÓN A VERIFICAR/AÑADIR ---

    @POST("com.atproto.repo.createRecord")
    suspend fun createRecord(
        @Header("Authorization") authorization: String,
        @Body input: CreateRecordInput
    ): Response<CreateRecordOutput>

    @Headers("Content-Type: image/jpeg") // O el que uses
    @POST("com.atproto.repo.uploadBlob")
    suspend fun uploadBlob(
        @Header("Authorization") authorization: String,
        @Body imageBytes: RequestBody
    ): Response<UploadBlobOutput>
}

