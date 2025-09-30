package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

// Puedes expandir esto si necesitas más campos de la respuesta de login
data class LoginResponse(
    @SerializedName("accessJwt") val accessJwt: String,
    @SerializedName("did") val did: String,
    @SerializedName("handle") val handle: String? = null, // Es bueno tener el handle
    @SerializedName("refreshJwt") val refreshJwt: String? = null // Y el refresh token
    // Otros campos como email, didDoc, etc., pueden ser añadidos si son necesarios
)