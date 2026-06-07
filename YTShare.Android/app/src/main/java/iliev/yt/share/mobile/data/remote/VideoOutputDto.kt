package iliev.yt.share.mobile.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class VideoOutputDto(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String
)
