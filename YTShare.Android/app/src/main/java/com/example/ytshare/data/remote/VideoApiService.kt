package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class VideoApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/videos"

    suspend fun getAllVideos(): List<VideoOutputDto> {
        return client.get("$baseUrl/all").body()
    }

    suspend fun getVideoById(id: String): VideoOutputDto {
        return client.get("$baseUrl/$id").body()
    }

    suspend fun createVideo(input: VideoInputDto): VideoOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    suspend fun deleteVideo(id: String) {
        client.delete("$baseUrl/$id")
    }

    suspend fun deleteAllVideos() {
        client.delete(baseUrl)
    }
}
