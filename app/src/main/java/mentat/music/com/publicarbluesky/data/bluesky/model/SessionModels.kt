package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

/**
 * Datos de entrada para el endpoint com.atproto.server.createSession.
 */
data class CreateSessionInput(
    @SerializedName("identifier")
    val identifier: String, // Puede ser el handle o el email
    @SerializedName("password")
    val password: String   // La contraseña de aplicación
)

/**
 * Datos de salida del endpoint com.atproto.server.createSession.
 */
data class CreateSessionOutput(
    @SerializedName("accessJwt")
    val accessJwt: String,

    @SerializedName("refreshJwt")
    val refreshJwt: String,

    @SerializedName("handle")
    val handle: String,

    @SerializedName("did")
    val did: String,

    @SerializedName("email") // Opcional, puede no estar siempre presente
    val email: String? = null
    // Puedes añadir otros campos si los necesitas, como "emailConfirmed"
)

