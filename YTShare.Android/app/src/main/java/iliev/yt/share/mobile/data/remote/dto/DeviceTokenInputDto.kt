package iliev.yt.share.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenInputDto(
    val fcmToken: String,
    val platform: String
)
