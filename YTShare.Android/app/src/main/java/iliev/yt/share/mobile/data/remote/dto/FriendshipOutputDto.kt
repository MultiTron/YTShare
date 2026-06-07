package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipOutputDto(
    val id: String,
    val user: UserOutputDto,
    val friend: UserOutputDto,
    val status: String
)
