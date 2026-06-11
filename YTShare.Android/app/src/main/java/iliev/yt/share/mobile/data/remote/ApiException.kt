package iliev.yt.share.mobile.data.remote

import io.ktor.http.HttpStatusCode

class ApiException(
    val statusCode: HttpStatusCode,
    val responseBody: String
) : Exception("HTTP ${statusCode.value} ${statusCode.description}: $responseBody")
