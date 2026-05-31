package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageInputDto(
    val content: String,
    val status: String,
    val chatId: String,
    val senderId: String
)
