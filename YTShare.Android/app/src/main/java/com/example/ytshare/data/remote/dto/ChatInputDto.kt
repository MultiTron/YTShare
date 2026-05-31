package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatInputDto(
    val participantIds: List<String>
)
