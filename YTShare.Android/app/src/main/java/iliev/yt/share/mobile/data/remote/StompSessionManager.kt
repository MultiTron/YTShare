package iliev.yt.share.mobile.data.remote

import android.util.Log
import iliev.yt.share.mobile.Constants
import iliev.yt.share.mobile.data.auth.AuthRepository
import iliev.yt.share.mobile.data.remote.dto.MessageOutputDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class StompSessionManager(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var session: StompSession? = null
    private var connectJob: Job? = null
    private val subscriptionJobs = mutableMapOf<String, Job>()
    private val messageFlows = mutableMapOf<String, MutableSharedFlow<MessageOutputDto>>()

    private val wsUrl: String
        get() {
            val baseUrl = Constants.BACKEND_BASE_URL.replace("/api", "")
            return "$baseUrl/ws/websocket"
        }

    fun connect() {
        if (session != null) return
        connectJob = scope.launch {
            var delayMs = 1000L
            while (true) {
                try {
                    val token = authRepository.getIdToken() ?: break
                    val okHttpClient = OkHttpClient.Builder().build()
                    val client = StompClient(OkHttpWebSocketClient(okHttpClient))
                    session = client.connect(
                        url = wsUrl,
                        customStompConnectHeaders = mapOf("Authorization" to "Bearer $token")
                    )
                    Log.d("StompSessionManager", "Connected to STOMP")
                    delayMs = 1000L
                    break
                } catch (e: Exception) {
                    Log.w("StompSessionManager", "STOMP connect failed, retrying in ${delayMs}ms", e)
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(30_000L)
                }
            }
        }
    }

    fun subscribe(chatId: String): Flow<MessageOutputDto> {
        val existing = messageFlows[chatId]
        if (existing != null) return existing

        val flow = MutableSharedFlow<MessageOutputDto>(extraBufferCapacity = 64)
        messageFlows[chatId] = flow

        val job = scope.launch {
            connectJob?.join()
            val currentSession = session ?: return@launch
            try {
                val textMessages = currentSession.subscribeText("/topic/chat/$chatId")
                textMessages.collect { text ->
                    val msg = json.decodeFromString<MessageOutputDto>(text)
                    flow.emit(msg)
                }
            } catch (e: Exception) {
                Log.w("StompSessionManager", "Subscription to chat $chatId failed", e)
            }
        }
        subscriptionJobs[chatId] = job

        return flow
    }

    fun unsubscribe(chatId: String) {
        subscriptionJobs.remove(chatId)?.cancel()
        messageFlows.remove(chatId)
    }

    fun disconnect() {
        subscriptionJobs.values.forEach { it.cancel() }
        subscriptionJobs.clear()
        messageFlows.clear()
        connectJob?.cancel()
        scope.launch {
            try {
                session?.disconnect()
            } catch (_: Exception) {}
            session = null
        }
    }
}
