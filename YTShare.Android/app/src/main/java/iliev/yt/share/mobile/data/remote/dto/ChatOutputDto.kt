package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatOutputDto(
    val id: String,
    val participants: List<UserOutputDto>
)
