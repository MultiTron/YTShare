package iliev.yt.share.mobile.data.remote

import iliev.yt.share.mobile.Constants
import iliev.yt.share.mobile.data.remote.dto.DeviceTokenInputDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class DeviceTokenApiService(private val client: HttpClient) {
    private val baseUrl = "${Constants.BACKEND_BASE_URL}/device-tokens"

    suspend fun registerToken(input: DeviceTokenInputDto) {
        client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }

    suspend fun removeToken() {
        client.delete(baseUrl)
    }
}
