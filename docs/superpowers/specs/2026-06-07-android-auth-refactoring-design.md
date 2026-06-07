# Android Auth Refactoring — Design Spec

## Goal

Refactor the Android app so that core functionality (sharing videos, viewing history, choosing a device) works without authentication. Add visibility into who is logged in and the ability to log out. Gate social features (Chat/Friends) and backend sync on authentication.

## Auth State Model

Replace the boolean `isAuthenticated: StateFlow<Boolean>` in `AuthViewModel` with a sealed class:

```kotlin
sealed class AuthState {
    object Anonymous : AuthState()
    data class Authenticated(val email: String) : AuthState()
}
```

- `AuthViewModel` exposes `val authState: StateFlow<AuthState>`.
- On sign-in/sign-up success: emit `Authenticated(email)` using `FirebaseAuth.currentUser.email`.
- On sign-out: emit `Anonymous`.
- On app start: check `FirebaseAuth.currentUser` — non-null emits `Authenticated`, null emits `Anonymous`.
- A convenience `val isAuthenticated: Boolean` computed property may exist for simple checks.

## Navigation Restructure

### Remove the auth gate

The current if/else branch in `MainActivityCompose.onCreate()` that switches between `LoginScreen` and `MainScreen` is removed. The app always renders `MainScreen` with the full NavHost.

### Login as a nav route

- Add `"login"` as a composable destination in the NavHost.
- The bottom navigation bar is hidden on the login route (same pattern used for the conversation screen).

### Bottom nav bar — Chat tab

- The Chat tab remains visible at all times.
- When `authState` is `Anonymous`, the Chat tab is greyed out and non-tappable (disabled).
- Home, History, and Settings tabs always function normally regardless of auth state.

## Settings Screen — Account Section

A new account section is added at the top of the Settings screen, above the existing device list and tracking toggle.

### Authenticated state

- Displays the user's email address.
- Shows a "Sign out" button.
- Tapping "Sign out" calls `authViewModel.signOut()`.
- The section updates in-place to the anonymous state.
- The Chat tab in the bottom nav greys out.
- The user stays on the Settings screen.

### Anonymous state

- Shows a "Sign in" button.
- Tapping "Sign in" navigates to the `"login"` route.
- On successful login, the nav pops back to Settings where the account section now shows the email and sign out button.

## LoginScreen Adjustments

- Becomes a regular composable destination within the NavHost (no longer a separate UI branch).
- Adds a top app bar with a back arrow that navigates back to the previous screen.
- On successful sign-in or sign-up, automatically calls `navController.popBackStack()` to return.
- No other functional changes — same email/password form, same toggle between login and register modes.

## Video History Sync

### Local history

`HistoryScreen` reads from Room DB via `VideoDao`. This is unchanged and works without authentication.

### Backend sync gating

- When a video is shared, it is always saved locally to Room.
- The call to `VideoApiService` to sync to the backend only executes if `authState` is `Authenticated`.
- A `synced` boolean column is added to `VideoEntity` in Room, defaulting to `false`.
- On login, the app queries for unsynced videos (`synced = false`) and pushes them to the backend via `VideoApiService`.
- On successful sync, each video's `synced` flag is set to `true`.
- A Room database migration is required for the new column.

## Feature Access Matrix

| Feature | Anonymous | Authenticated |
|---|---|---|
| Share video to device | Yes | Yes |
| View local video history | Yes | Yes |
| Choose device (Settings) | Yes | Yes |
| Toggle tracking removal | Yes | Yes |
| Sync video history to backend | No (queued) | Yes (auto-sync) |
| Chat / Friends | No (tab disabled) | Yes |
| View logged-in email | N/A | Yes (Settings) |
| Log out | N/A | Yes (Settings) |

## Files to Modify

| File | Change |
|---|---|
| `AuthRepository.kt` | Return email from sign-in/sign-up results |
| `AuthViewModel.kt` | New `AuthState` sealed class, replace `isAuthenticated` StateFlow |
| `MainActivityCompose.kt` | Remove auth gate, add `"login"` route, disable Chat tab when anonymous |
| `LoginScreen.kt` | Add top app bar with back arrow, pop back on success |
| `SettingsScreen.kt` | Add account section (email + sign out / sign in) |
| `SettingsViewModel.kt` | Expose auth state for the account section |
| `VideoEntity.kt` | Add `synced` boolean column |
| `AppDatabase.kt` | Room migration for new column |
| `HomeScreen.kt` | Gate `VideoApiService` sync call on auth state |

## Files Not Changed

- `HistoryScreen.kt` — reads from local Room DB, no auth dependency
- `FriendsScreen.kt` — already behind the disabled Chat tab
- `ConversationScreen.kt` — already behind the disabled Chat tab
- Backend code — no API changes needed
