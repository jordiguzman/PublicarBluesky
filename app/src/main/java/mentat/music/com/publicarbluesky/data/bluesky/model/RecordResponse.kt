package mentat.music.com.publicarbluesky.data.bluesky.model

import com.google.gson.annotations.SerializedName

data class RecordResponse(
    @SerializedName("uri") val uri: String,
    @SerializedName("cid") val cid: String
)