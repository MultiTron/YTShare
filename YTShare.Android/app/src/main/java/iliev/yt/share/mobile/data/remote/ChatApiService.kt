package iliev.yt.share.mobile.data.remote

import iliev.yt.share.mobile.Constants
import iliev.yt.share.mobile.data.remote.dto.ChatInputDto
import iliev.yt.share.mobile.data.remote.dto.ChatOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ChatApiService(private val client: HttpClient) {
    private val baseUrl = "${Constants.BACKEND_BASE_URL}/chats"

    suspend fun getAllChats(): List<ChatOutputDto> {
        return client.get("$baseUrl/all").body()
    }

    suspend fun createChat(input: ChatInputDto): ChatOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
