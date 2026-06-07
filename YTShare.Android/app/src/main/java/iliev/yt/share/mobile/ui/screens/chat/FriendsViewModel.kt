package iliev.yt.share.mobile.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iliev.yt.share.mobile.data.remote.dto.FriendshipOutputDto
import iliev.yt.share.mobile.data.remote.dto.UserOutputDto
import iliev.yt.share.mobile.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _friends = MutableStateFlow<List<FriendshipOutputDto>>(emptyList())
    val friends: StateFlow<List<FriendshipOutputDto>> = _friends

    private val _pendingRequests = MutableStateFlow<List<FriendshipOutputDto>>(emptyList())
    val pendingRequests: StateFlow<List<FriendshipOutputDto>> = _pendingRequests

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<UserOutputDto?>(null)
    val currentUser: StateFlow<UserOutputDto?> = _currentUser

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentUser.value = repository.getCurrentUser()
                _friends.value = repository.getFriends()
                _pendingRequests.value = repository.getPendingRequests()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to load data", e)
                _snackbarMessage.value = "Couldn't load friends. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(email: String) {
        viewModelScope.launch {
            try {
                val currentEmail = _currentUser.value?.email
                if (email.equals(currentEmail, ignoreCase = true)) {
                    _snackbarMessage.value = "You can't add yourself as a friend"
                    return@launch
                }
                repository.sendFriendRequest(email)
                _snackbarMessage.value = "Friend request sent!"
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to send request", e)
                _snackbarMessage.value = "No user found with that email"
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to accept", e)
                _snackbarMessage.value = "Failed to accept request"
            }
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.rejectFriendRequest(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to reject", e)
                _snackbarMessage.value = "Failed to reject request"
            }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.removeFriend(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to remove friend", e)
                _snackbarMessage.value = "Failed to remove friend"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun isIncomingRequest(friendship: FriendshipOutputDto): Boolean {
        return _currentUser.value?.let { friendship.friend.id == it.id } ?: false
    }

    fun getFriendUser(friendship: FriendshipOutputDto): UserOutputDto {
        val current = _currentUser.value
        return if (current != null && friendship.user.id == current.id) friendship.friend else friendship.user
    }
}
