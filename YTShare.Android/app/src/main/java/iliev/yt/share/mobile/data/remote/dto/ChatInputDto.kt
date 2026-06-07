package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatInputDto(
    val participantIds: List<String>
)
