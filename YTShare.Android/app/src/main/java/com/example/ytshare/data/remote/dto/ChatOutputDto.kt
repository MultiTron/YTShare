package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatOutputDto(
    val id: String,
    val participants: List<UserOutputDto>
)
