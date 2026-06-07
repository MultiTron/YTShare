package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserOutputDto(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
