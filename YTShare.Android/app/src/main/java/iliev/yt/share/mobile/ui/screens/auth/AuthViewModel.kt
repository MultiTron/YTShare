package iliev.yt.share.mobile.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iliev.yt.share.mobile.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(
        authRepository.currentUser?.email?.let { AuthState.Authenticated(it) }
            ?: AuthState.Anonymous
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signIn(email, password)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user.email ?: email)
            }.onFailure { e ->
                _error.value = e.message ?: "Sign in failed"
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signUp(email, password)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user.email ?: email)
            }.onFailure { e ->
                _error.value = e.message ?: "Sign up failed"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Anonymous
    }
}
