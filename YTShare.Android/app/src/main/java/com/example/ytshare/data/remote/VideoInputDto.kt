package com.example.ytshare.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class VideoInputDto(
    val title: String,
    val url: String,
    val thumbnailUrl: String
)
