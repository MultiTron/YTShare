package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipInputDto(
    val userId: String,
    val friendId: String,
    val status: String
)
