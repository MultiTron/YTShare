# Android Auth Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow the Android app's core features (share video, history, device selection) to work without login. Show logged-in user info and logout in Settings. Gate Chat/Friends and backend sync on authentication.

**Architecture:** Replace the boolean auth gate in `MainActivityCompose` with a sealed `AuthState` model. The app always renders `MainScreen`. LoginScreen becomes a nav route. Settings gains an account section. `VideoRepository` checks auth state before syncing to backend, and syncs unsynced videos on login.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Navigation, Firebase Auth, Koin DI, Room, Ktor

**Spec:** `docs/superpowers/specs/2026-06-07-android-auth-refactoring-design.md`

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `ui/screens/auth/AuthState.kt` | Create | Sealed class: `Anonymous`, `Authenticated(email)` |
| `ui/screens/auth/AuthViewModel.kt` | Modify | Replace `_isAuthenticated: StateFlow<Boolean>` with `_authState: StateFlow<AuthState>` |
| `ui/screens/auth/LoginScreen.kt` | Modify | Accept `navController`, add top app bar with back arrow, pop on success |
| `MainActivityCompose.kt` | Modify | Remove auth gate, add `"login"` route, disable Chat tab when anonymous, gate STOMP/FCM on auth |
| `ui/screens/SettingsScreen.kt` | Modify | Add account section (email + sign out / sign in button) |
| `ui/screens/SettingsViewModel.kt` | Modify | Accept `AuthViewModel`, expose auth state |
| `data/repository/VideoRepository.kt` | Modify | Accept `AuthRepository`, gate backend calls on auth |
| `di/AppModules.kt` | Modify | Update DI wiring for new dependencies |

Files NOT changed: `AuthRepository.kt` (already returns `FirebaseUser` which has `.email`), `VideoEntity.kt` (already has `synced` field), `VideoDao.kt` (already has `getUnsyncedVideos`), `AppDatabase.kt` (no migration needed), `HomeScreen.kt` (no changes — `VideoRepository` handles gating internally), `HistoryScreen.kt`, `FriendsScreen.kt`, `ConversationScreen.kt`.

---

### Task 1: Create AuthState sealed class

**Files:**
- Create: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/AuthState.kt`

- [ ] **Step 1: Create AuthState.kt**

```kotlin
package iliev.yt.share.mobile.ui.screens.auth

sealed class AuthState {
    object Anonymous : AuthState()
    data class Authenticated(val email: String) : AuthState()

    val isAuthenticated: Boolean
        get() = this is Authenticated
}
```

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/AuthState.kt
git commit -m "feat: add AuthState sealed class"
```

---

### Task 2: Refactor AuthViewModel to use AuthState

**Files:**
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/AuthViewModel.kt`

- [ ] **Step 1: Replace the full AuthViewModel implementation**

Replace the entire contents of `AuthViewModel.kt` with:

```kotlin
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
```

Key changes from the original:
- `_isAuthenticated: MutableStateFlow<Boolean>` → `_authState: MutableStateFlow<AuthState>`
- Init reads `authRepository.currentUser?.email` to build initial state
- `signIn`/`signUp` emit `AuthState.Authenticated(user.email ?: email)` on success (fallback to the input email if Firebase user email is null for any reason)
- `signOut` emits `AuthState.Anonymous`

- [ ] **Step 2: Verify the project compiles**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: compilation errors in `MainActivityCompose.kt` referencing `authViewModel.isAuthenticated` — this is correct, we fix it in Task 4. No errors in `AuthViewModel.kt` itself.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/AuthViewModel.kt
git commit -m "refactor: replace boolean isAuthenticated with AuthState sealed class in AuthViewModel"
```

---

### Task 3: Refactor LoginScreen for in-nav usage

**Files:**
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/LoginScreen.kt`

- [ ] **Step 1: Replace the full LoginScreen implementation**

Replace the entire contents of `LoginScreen.kt` with:

```kotlin
package iliev.yt.share.mobile.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AuthViewModel, navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState.isAuthenticated) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "Register" else "Sign In") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Red,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "YTShare",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRegisterMode) "Create an account" else "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRegisterMode) {
                        viewModel.signUp(email, password)
                    } else {
                        viewModel.signIn(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(if (isRegisterMode) "Register" else "Sign In")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode) "Already have an account? Sign in"
                    else "Don't have an account? Register"
                )
            }
        }
    }
}
```

Key changes from the original:
- Added `navController: NavController` parameter
- Added `Scaffold` with `TopAppBar` (red, with back arrow)
- Added `LaunchedEffect(authState)` that pops back when auth succeeds
- Observes `authState` instead of `isAuthenticated`
- Form content wrapped in Scaffold's padding

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/auth/LoginScreen.kt
git commit -m "refactor: adapt LoginScreen for in-nav usage with top app bar and back navigation"
```

---

### Task 4: Refactor MainActivityCompose — remove auth gate, add login route, disable Chat tab

**Files:**
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/MainActivityCompose.kt`

- [ ] **Step 1: Replace the full MainActivityCompose implementation**

Replace the entire contents of `MainActivityCompose.kt` with:

```kotlin
package iliev.yt.share.mobile

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import iliev.yt.share.mobile.data.remote.StompSessionManager
import iliev.yt.share.mobile.data.repository.ChatRepository
import iliev.yt.share.mobile.data.repository.VideoRepository
import iliev.yt.share.mobile.helpers.NSDHelper
import iliev.yt.share.mobile.helpers.SharedPrefHelper
import iliev.yt.share.mobile.ui.screens.HomeScreen
import iliev.yt.share.mobile.ui.screens.SettingsScreen
import iliev.yt.share.mobile.ui.screens.auth.AuthState
import iliev.yt.share.mobile.ui.screens.auth.AuthViewModel
import iliev.yt.share.mobile.ui.screens.auth.LoginScreen
import iliev.yt.share.mobile.ui.screens.chat.ConversationScreen
import iliev.yt.share.mobile.ui.screens.chat.ConversationViewModel
import iliev.yt.share.mobile.ui.screens.chat.FriendsScreen
import iliev.yt.share.mobile.ui.screens.chat.FriendsViewModel
import iliev.yt.share.mobile.ui.screens.history.HistoryScreen
import iliev.yt.share.mobile.ui.screens.history.HistoryViewModel
import iliev.yt.share.mobile.ui.theme.YTShareTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivityCompose : ComponentActivity() {

    private val nsd: NSDHelper by inject()
    private val sharedPref: SharedPreferences by inject()
    lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        queue = Volley.newRequestQueue(this)

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }

        setContent {
            YTShareTheme {
                val authViewModel: AuthViewModel = koinViewModel()
                MainScreen(authViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nsd.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        nsd.stopDiscovery()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    fun MainScreen(authViewModel: AuthViewModel) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val authState by authViewModel.authState.collectAsState()

        val stompManager: StompSessionManager = org.koin.compose.koinInject()
        val chatRepo: ChatRepository = org.koin.compose.koinInject()
        val videoRepo: VideoRepository = org.koin.compose.koinInject()

        LaunchedEffect(authState) {
            if (authState is AuthState.Authenticated) {
                stompManager.connect()
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try { chatRepo.registerDeviceToken(token) } catch (e: Exception) { Log.w("FCM", "Failed to register device token", e) }
                        }
                    }
                } catch (e: Exception) { Log.w("FCM", "Failed to get FCM token", e) }

                try { videoRepo.syncUnsyncedVideos() } catch (e: Exception) { Log.w("Sync", "Failed to sync videos", e) }
            }
        }

        var ipAddress by remember { mutableStateOf(sharedPref.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0") }
        var savedLink by remember { mutableStateOf(sharedPref.getString(Constants.link, "")) }
        var isTracking by remember { mutableStateOf(sharedPref.getBoolean(Constants.isTracking, false)) }

        val hosts by nsd.hosts.collectAsState()
        LaunchedEffect(hosts) {
            if (ipAddress == "0.0.0.0" && hosts.isNotEmpty()) {
                val host = hosts.first()
                SharedPrefHelper.saveIp(host.toString(), sharedPref)
                ipAddress = host.toString()
            }
        }

        val hideBottomBar = currentRoute?.startsWith("conversation") == true || currentRoute == "login"

        Scaffold(
            bottomBar = {
                if (!hideBottomBar) NavigationBar(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEach { item ->
                        val isChatTab = item.route == "friends"
                        val isDisabled = isChatTab && !authState.isAuthenticated
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(Dp(30f))
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            enabled = !isDisabled,
                            onClick = {
                                if (!isDisabled) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = if (isDisabled) Color(0x66FFFFFF) else Color.White,
                                selectedTextColor = Color.White,
                                unselectedTextColor = if (isDisabled) Color(0x66FFFFFF) else Color.White,
                                indicatorColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeScreen(
                        savedLink = savedLink,
                        ipAddress = ipAddress,
                        isTracking = isTracking,
                        queue = queue,
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onClearLink = {
                            SharedPrefHelper.clearLink(sharedPref)
                            savedLink = ""
                        }
                    )
                }

                composable("history") {
                    val historyViewModel: HistoryViewModel = koinViewModel()
                    HistoryScreen(viewModel = historyViewModel)
                }

                composable("settings") {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        navController = navController,
                        onIpChanged = { newIp ->
                            ipAddress = newIp
                        }
                    )
                }

                composable("friends") {
                    val friendsViewModel: FriendsViewModel = koinViewModel()
                    FriendsScreen(
                        viewModel = friendsViewModel,
                        onFriendClick = { friendUserId ->
                            val friendship = friendsViewModel.friends.value.find { f ->
                                friendsViewModel.getFriendUser(f).id == friendUserId
                            }
                            val friendUser = friendship?.let { friendsViewModel.getFriendUser(it) }
                            val firstName = friendUser?.firstName ?: ""
                            val lastName = friendUser?.lastName ?: ""
                            navController.navigate("conversation/$friendUserId/$firstName/$lastName")
                        }
                    )
                }

                composable(
                    route = "conversation/{friendId}/{firstName}/{lastName}",
                    arguments = listOf(
                        navArgument("friendId") { type = NavType.StringType },
                        navArgument("firstName") { type = NavType.StringType },
                        navArgument("lastName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
                    val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
                    val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
                    val conversationViewModel: ConversationViewModel = koinViewModel()
                    ConversationScreen(
                        viewModel = conversationViewModel,
                        friendId = friendId,
                        friendFirstName = firstName,
                        friendLastName = lastName,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        navController = navController
                    )
                }
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            SharedPrefHelper.saveLink(it, sharedPref)
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Filled.Home, "Home"),
    BottomNavItem("history", Icons.Filled.History, "History"),
    BottomNavItem("friends", Icons.AutoMirrored.Filled.Chat, "Chat"),
    BottomNavItem("settings", Icons.Filled.Settings, "Settings")
)
```

Key changes from the original:
- **Auth gate removed:** No more `if (isAuthenticated) MainScreen() else LoginScreen()` — always renders `MainScreen`
- **`"login"` route added** to NavHost, renders `LoginScreen(viewModel, navController)`
- **Bottom bar hidden** on both `"conversation"` and `"login"` routes
- **Chat tab disabled** when `authState` is `Anonymous` — greyed out (`0x66FFFFFF` = 40% white) and `enabled = false`
- **STOMP/FCM gated on auth:** `LaunchedEffect(authState)` only connects STOMP, registers FCM token, and syncs unsynced videos when `authState is Authenticated`
- **SettingsScreen** now receives `authViewModel` and `navController` parameters
- **VideoRepository** injected via Koin to call `syncUnsyncedVideos()` on login

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/MainActivityCompose.kt
git commit -m "refactor: remove auth gate, add login route, disable chat tab when anonymous"
```

---

### Task 5: Add account section to SettingsScreen

**Files:**
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Replace the full SettingsScreen implementation**

Replace the entire contents of `SettingsScreen.kt` with:

```kotlin
package iliev.yt.share.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import iliev.yt.share.mobile.models.HostModel
import iliev.yt.share.mobile.ui.screens.auth.AuthState
import iliev.yt.share.mobile.ui.screens.auth.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    onIpChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val hosts by viewModel.hosts.collectAsState()
    val isTrackingEnabled by viewModel.isTrackingEnabled.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Account section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Account",
                    modifier = Modifier.size(40.dp),
                    tint = if (authState.isAuthenticated) Color.Red else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                when (authState) {
                    is AuthState.Authenticated -> {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (authState as AuthState.Authenticated).email,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = { authViewModel.signOut() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Sign Out")
                        }
                    }
                    is AuthState.Anonymous -> {
                        Text(
                            text = "Not signed in",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { navController.navigate("login") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }

        Text(
            text = "Available Devices",
            fontSize = 30.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(hosts) { host ->
                    HostCard(
                        host = host,
                        onHostClick = {
                            viewModel.selectHost(host)
                            onIpChanged(host.toString())
                            Toast.makeText(context, "Setting selected ip...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setTracking(!isTrackingEnabled) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTrackingEnabled,
                onCheckedChange = { viewModel.setTracking(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Remove tracking from URLs",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun HostCard(
    host: HostModel,
    onHostClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onHostClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = host.hostName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp)
            )
            Text(
                text = host.toString(),
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
```

Key changes from the original:
- Added `authViewModel: AuthViewModel` and `navController: NavController` parameters
- New account `Card` at the top with:
  - `AccountCircle` icon (red when authenticated, gray when anonymous)
  - Authenticated: shows email + "Sign Out" button
  - Anonymous: shows "Not signed in" + "Sign In" button that navigates to `"login"` route

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/ui/screens/SettingsScreen.kt
git commit -m "feat: add account section to SettingsScreen with sign in/out"
```

---

### Task 6: Gate VideoRepository backend calls on auth state

**Files:**
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/data/repository/VideoRepository.kt`
- Modify: `YTShare.Android/app/src/main/java/iliev/yt/share/mobile/di/AppModules.kt`

- [ ] **Step 1: Add AuthRepository dependency to VideoRepository**

Replace the entire contents of `VideoRepository.kt` with:

```kotlin
package iliev.yt.share.mobile.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import iliev.yt.share.mobile.data.auth.AuthRepository
import iliev.yt.share.mobile.data.local.VideoDao
import iliev.yt.share.mobile.data.local.VideoEntity
import iliev.yt.share.mobile.data.remote.VideoApiService
import iliev.yt.share.mobile.data.remote.VideoInputDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VideoRepository(
    private val api: VideoApiService,
    private val dao: VideoDao,
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "VideoRepository"
        private val LAST_SYNC_KEY = longPreferencesKey("last_sync_timestamp")
    }

    fun getVideos(): Flow<List<VideoEntity>> {
        return dao.getAllVideos()
    }

    fun searchVideos(query: String): Flow<List<VideoEntity>> {
        return dao.searchVideos(query)
    }

    suspend fun refreshFromBackend() {
        if (!authRepository.isAuthenticated) return
        try {
            val remoteVideos = api.getAllVideos()
            val entities = remoteVideos.map { dto ->
                VideoEntity(
                    id = dto.id,
                    title = dto.title,
                    url = dto.url,
                    thumbnailUrl = dto.thumbnailUrl,
                    createdAt = System.currentTimeMillis(),
                    synced = true
                )
            }
            dao.deleteAll()
            dao.insertAll(entities)
            updateLastSync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh from backend", e)
        }
    }

    suspend fun saveVideo(input: VideoInputDto) {
        if (!authRepository.isAuthenticated) {
            val entity = VideoEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = input.title,
                url = input.url,
                thumbnailUrl = input.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
                synced = false
            )
            dao.insert(entity)
            return
        }
        try {
            val response = api.createVideo(input)
            val entity = VideoEntity(
                id = response.id,
                title = response.title,
                url = response.url,
                thumbnailUrl = response.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
                synced = true
            )
            dao.insert(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to backend, caching locally", e)
            val entity = VideoEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = input.title,
                url = input.url,
                thumbnailUrl = input.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
                synced = false
            )
            dao.insert(entity)
        }
    }

    suspend fun deleteVideo(id: String) {
        if (authRepository.isAuthenticated) {
            try {
                api.deleteVideo(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete from backend", e)
            }
        }
        dao.deleteById(id)
    }

    suspend fun deleteAllVideos() {
        if (authRepository.isAuthenticated) {
            try {
                api.deleteAllVideos()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete all from backend", e)
            }
        }
        dao.deleteAll()
    }

    suspend fun syncUnsyncedVideos() {
        val unsynced = dao.getUnsyncedVideos()
        for (video in unsynced) {
            try {
                val input = VideoInputDto(
                    title = video.title,
                    url = video.url,
                    thumbnailUrl = video.thumbnailUrl
                )
                val response = api.createVideo(input)
                dao.deleteById(video.id)
                val synced = VideoEntity(
                    id = response.id,
                    title = response.title,
                    url = response.url,
                    thumbnailUrl = response.thumbnailUrl,
                    createdAt = video.createdAt,
                    synced = true
                )
                dao.insert(synced)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync video ${video.id}", e)
                break
            }
        }
    }

    suspend fun needsFullSync(): Boolean {
        val lastSync = dataStore.data.map { prefs ->
            prefs[LAST_SYNC_KEY] ?: 0L
        }.first()
        return lastSync == 0L
    }

    private suspend fun updateLastSync() {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_KEY] = System.currentTimeMillis()
        }
    }
}
```

Key changes from the original:
- Added `authRepository: AuthRepository` constructor parameter
- `saveVideo`: if not authenticated, saves locally with `synced = false` immediately (no backend attempt)
- `deleteVideo`/`deleteAllVideos`: only call backend API if authenticated
- `refreshFromBackend`: early return if not authenticated
- `syncUnsyncedVideos`: unchanged (already correct — only called from `LaunchedEffect` when authenticated)

- [ ] **Step 2: Update Koin module to pass AuthRepository to VideoRepository**

In `AppModules.kt`, change the `VideoRepository` binding in `repositoryModule` from:

```kotlin
    single { VideoRepository(get(), get(), androidContext().dataStore) }
```

to:

```kotlin
    single { VideoRepository(get(), get(), androidContext().dataStore, get()) }
```

This passes the existing `AuthRepository` singleton (already in the module) as the 4th parameter.

- [ ] **Step 3: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS with no errors.

- [ ] **Step 4: Commit**

```bash
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/data/repository/VideoRepository.kt
git add YTShare.Android/app/src/main/java/iliev/yt/share/mobile/di/AppModules.kt
git commit -m "feat: gate VideoRepository backend calls on auth state"
```

---

### Task 7: Final build verification

- [ ] **Step 1: Full debug build**

Run from `YTShare.Android/`:

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESS, APK generated at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Commit any remaining changes (if any)**

If the build revealed any issues that were fixed, commit those fixes.

---

## Summary of Changes

| Task | What changes | Commit message |
|---|---|---|
| 1 | New `AuthState` sealed class | `feat: add AuthState sealed class` |
| 2 | `AuthViewModel` uses `AuthState` instead of boolean | `refactor: replace boolean isAuthenticated with AuthState sealed class in AuthViewModel` |
| 3 | `LoginScreen` accepts `navController`, adds top app bar | `refactor: adapt LoginScreen for in-nav usage with top app bar and back navigation` |
| 4 | `MainActivityCompose` removes auth gate, adds login route, disables Chat tab | `refactor: remove auth gate, add login route, disable chat tab when anonymous` |
| 5 | `SettingsScreen` gains account section | `feat: add account section to SettingsScreen with sign in/out` |
| 6 | `VideoRepository` gates backend on auth, Koin wiring updated | `feat: gate VideoRepository backend calls on auth state` |
| 7 | Final build verification | — |
