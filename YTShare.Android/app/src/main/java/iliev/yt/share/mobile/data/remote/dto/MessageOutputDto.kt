package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageOutputDto(
    val id: String,
    val content: String,
    val status: String,
    val chat: ChatOutputDto,
    val sender: UserOutputDto,
    val createdAt: String
)
