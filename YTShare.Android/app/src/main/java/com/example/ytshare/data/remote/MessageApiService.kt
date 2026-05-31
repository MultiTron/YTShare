package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.MessageInputDto
import com.example.ytshare.data.remote.dto.MessageOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MessageApiService(private val client: HttpClient) {
    private val baseUrl = "${Constants.BACKEND_BASE_URL}/messages"

    suspend fun getMessagesByChat(chatId: String): List<MessageOutputDto> {
        return client.get("$baseUrl/chat/$chatId").body()
    }

    suspend fun sendMessage(input: MessageInputDto): MessageOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
