package iliev.yt.share.mobile.ui.screens.auth

sealed class AuthState {
    object Anonymous : AuthState()
    data class Authenticated(val email: String) : AuthState()

    val isAuthenticated: Boolean
        get() = this is Authenticated
}
