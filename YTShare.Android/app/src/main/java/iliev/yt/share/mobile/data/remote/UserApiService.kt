package iliev.yt.share.mobile.data.remote

import iliev.yt.share.mobile.Constants
import iliev.yt.share.mobile.data.remote.dto.UserOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UserApiService(private val client: HttpClient) {
    private val baseUrl = "${Constants.BACKEND_BASE_URL}/users"

    suspend fun getCurrentUser(): UserOutputDto {
        return client.get("$baseUrl/me").body()
    }

    suspend fun getUserByEmail(email: String): UserOutputDto {
        return client.get("$baseUrl/by-email") {
            parameter("email", email)
        }.body()
    }
}
