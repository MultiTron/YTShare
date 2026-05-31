package com.example.ytshare.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.data.remote.StompSessionManager
import com.example.ytshare.data.remote.dto.MessageOutputDto
import com.example.ytshare.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val repository: ChatRepository,
    private val stompSessionManager: StompSessionManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageOutputDto>>(emptyList())
    val messages: StateFlow<List<MessageOutputDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _friendName = MutableStateFlow("")
    val friendName: StateFlow<String> = _friendName

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private var chatId: String? = null
    private var currentUserId: String? = null
    private var subscriptionJob: Job? = null

    fun load(friendId: String, friendFirstName: String, friendLastName: String) {
        _friendName.value = "$friendFirstName $friendLastName"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.getCurrentUser()
                currentUserId = user.id
                val chat = repository.getOrCreateChat(friendId)
                chatId = chat.id
                _messages.value = repository.getMessages(chat.id)
                subscribe()
            } catch (e: Exception) {
                Log.e("ConversationVM", "Failed to load messages", e)
                _snackbarMessage.value = "Couldn't load messages. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun subscribe() {
        val id = chatId ?: return
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            stompSessionManager.subscribe(id).collect { msg ->
                val current = _messages.value
                if (current.none { it.id == msg.id }) {
                    _messages.value = current + msg
                }
            }
        }
    }

    fun unsubscribe() {
        subscriptionJob?.cancel()
        chatId?.let { stompSessionManager.unsubscribe(it) }
    }

    fun sendMessage(content: String) {
        val id = chatId ?: return
        viewModelScope.launch {
            try {
                repository.sendMessage(id, content)
            } catch (e: Exception) {
                Log.e("ConversationVM", "Failed to send message", e)
                _snackbarMessage.value = "Message failed to send."
            }
        }
    }

    fun isOwnMessage(msg: MessageOutputDto): Boolean {
        return msg.sender.id == currentUserId
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
    }
}
