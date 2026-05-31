package com.example.ytshare.data.repository

import com.example.ytshare.data.remote.ChatApiService
import com.example.ytshare.data.remote.DeviceTokenApiService
import com.example.ytshare.data.remote.FriendshipApiService
import com.example.ytshare.data.remote.MessageApiService
import com.example.ytshare.data.remote.UserApiService
import com.example.ytshare.data.remote.dto.ChatInputDto
import com.example.ytshare.data.remote.dto.ChatOutputDto
import com.example.ytshare.data.remote.dto.DeviceTokenInputDto
import com.example.ytshare.data.remote.dto.FriendshipInputDto
import com.example.ytshare.data.remote.dto.FriendshipOutputDto
import com.example.ytshare.data.remote.dto.MessageInputDto
import com.example.ytshare.data.remote.dto.MessageOutputDto
import com.example.ytshare.data.remote.dto.UserOutputDto

class ChatRepository(
    private val userApi: UserApiService,
    private val friendshipApi: FriendshipApiService,
    private val chatApi: ChatApiService,
    private val messageApi: MessageApiService,
    private val deviceTokenApi: DeviceTokenApiService
) {
    private var cachedCurrentUser: UserOutputDto? = null

    suspend fun getCurrentUser(): UserOutputDto {
        cachedCurrentUser?.let { return it }
        val user = userApi.getCurrentUser()
        cachedCurrentUser = user
        return user
    }

    fun clearCachedUser() {
        cachedCurrentUser = null
    }

    suspend fun getFriends(): List<FriendshipOutputDto> {
        val user = getCurrentUser()
        return friendshipApi.getFriendshipsByStatus(user.id, "ACCEPTED")
    }

    suspend fun getPendingRequests(): List<FriendshipOutputDto> {
        val user = getCurrentUser()
        return friendshipApi.getFriendshipsByStatus(user.id, "PENDING")
    }

    suspend fun sendFriendRequest(email: String) {
        val user = getCurrentUser()
        val foundUser = userApi.getUserByEmail(email)
        friendshipApi.sendFriendRequest(
            FriendshipInputDto(userId = user.id, friendId = foundUser.id, status = "PENDING")
        )
    }

    suspend fun acceptFriendRequest(friendshipId: String) {
        friendshipApi.updateFriendshipStatus(friendshipId, "ACCEPTED")
    }

    suspend fun rejectFriendRequest(friendshipId: String) {
        friendshipApi.updateFriendshipStatus(friendshipId, "REJECTED")
    }

    suspend fun removeFriend(friendshipId: String) {
        friendshipApi.deleteFriendship(friendshipId)
    }

    suspend fun getOrCreateChat(friendId: String): ChatOutputDto {
        val chats = chatApi.getAllChats()
        val existing = chats.find { chat ->
            chat.participants.any { it.id == friendId }
        }
        if (existing != null) return existing

        val user = getCurrentUser()
        return chatApi.createChat(ChatInputDto(participantIds = listOf(user.id, friendId)))
    }

    suspend fun getMessages(chatId: String): List<MessageOutputDto> {
        return messageApi.getMessagesByChat(chatId)
    }

    suspend fun sendMessage(chatId: String, content: String): MessageOutputDto {
        val user = getCurrentUser()
        return messageApi.sendMessage(
            MessageInputDto(content = content, status = "SENT", chatId = chatId, senderId = user.id)
        )
    }

    suspend fun registerDeviceToken(fcmToken: String) {
        deviceTokenApi.registerToken(DeviceTokenInputDto(fcmToken = fcmToken, platform = "ANDROID"))
    }

    suspend fun removeDeviceToken() {
        deviceTokenApi.removeToken()
    }
}
