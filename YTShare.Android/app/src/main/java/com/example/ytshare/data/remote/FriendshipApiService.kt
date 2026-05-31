package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.FriendshipInputDto
import com.example.ytshare.data.remote.dto.FriendshipOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FriendshipApiService(private val client: HttpClient) {
    private val baseUrl = "${Constants.BACKEND_BASE_URL}/friendships"

    suspend fun getFriendshipsByUser(userId: String): List<FriendshipOutputDto> {
        return client.get("$baseUrl/user/$userId").body()
    }

    suspend fun getFriendshipsByStatus(userId: String, status: String): List<FriendshipOutputDto> {
        return client.get("$baseUrl/user/$userId/status") {
            parameter("status", status)
        }.body()
    }

    suspend fun sendFriendRequest(input: FriendshipInputDto): FriendshipOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    suspend fun updateFriendshipStatus(id: String, status: String): FriendshipOutputDto {
        return client.patch("$baseUrl/$id/status") {
            parameter("status", status)
        }.body()
    }

    suspend fun deleteFriendship(id: String) {
        client.delete("$baseUrl/$id")
    }
}
