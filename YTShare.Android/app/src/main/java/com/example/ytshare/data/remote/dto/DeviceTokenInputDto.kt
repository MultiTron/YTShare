package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenInputDto(
    val fcmToken: String,
    val platform: String
)
