# NSD Service Discovery Refactoring — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix hostname display, eliminate duplicate host cards, add lifecycle management, and align NSD code with the app's MVVM + Koin architecture.

**Architecture:** Host sends hostname via mDNS TXT record. Android `NSDHelper` rewrites to use `StateFlow`, lifecycle-aware discovery, and best-IP subnet matching. New `SettingsViewModel` follows the existing MVVM pattern. `MainActivityCompose` cleaned up to remove manual helper initialization.

**Tech Stack:** C#/.NET 8 (Bonjour COM), Kotlin (Android NSD API, Jetpack Compose, Koin DI, Coroutines/StateFlow)

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `YTShare.Host/YTShare.Server/Program.cs` | Modify | Add TXT record with hostname |
| `YTShare.Android/.../models/HostModel.kt` | Modify | Convert to immutable data class |
| `YTShare.Android/.../helpers/NSDHelper.kt` | Rewrite | StateFlow, lifecycle, best-IP, dedup |
| `YTShare.Android/.../ui/screens/SettingsViewModel.kt` | Create | MVVM for settings screen |
| `YTShare.Android/.../ui/screens/SettingsScreen.kt` | Modify | Use SettingsViewModel |
| `YTShare.Android/.../MainActivityCompose.kt` | Modify | Lifecycle binding, remove manual init |
| `YTShare.Android/.../di/AppModules.kt` | Modify | Register NSDHelper, SharedPrefs, SettingsViewModel |
| `YTShare.Android/.../ui/screens/HomeScreen.kt` | Modify | Remove unused `db` parameter |
| `YTShare.Android/.../adapters/HostAdapter.kt` | Delete | Dead code |

---

### Task 1: Add TXT Record to Host Service Registration

**Files:**
- Modify: `YTShare.Host/YTShare.Server/Program.cs:40-50`

- [ ] **Step 1: Add TXT record with hostname to Bonjour registration**

Replace the `RegisterBonjourService` method body in `Program.cs`:

```csharp
void RegisterBonjourService()
{
    try
    {
        DNSSDService bonjourService = new DNSSDService();
        DNSSDEventManager eventManager = new DNSSDEventManager();

        eventManager.ServiceRegistered += EventManager_ServiceRegistered;

        TXTRecord txtRecord = new TXTRecord();
        txtRecord.SetValue("hostname", Environment.MachineName);

        bonjourService.Register(
            0,                      // No flags
            0,                      // Interface index (0 = all interfaces)
            "YTShareService",       // Service name
            "_http._tcp.",          // Service type
            null,                   // Domain (null = local domain)
            null,                   // Host name (null = default)
            7296,                   // Port number
            txtRecord,              // TXT record with hostname
            eventManager            // Event manager for callbacks
        );
    }
    catch (Exception ex)
    {
        app.Logger.LogError($"Error: {ex.Message}");
    }
}
```

- [ ] **Step 2: Build the host project to verify compilation**

Run from `YTShare.Host/YTShare.Server/`:
```
dotnet build
```
Expected: Build succeeded, 0 errors.

- [ ] **Step 3: Commit**

```
git add YTShare.Host/YTShare.Server/Program.cs
git commit -m "feat(host): add hostname TXT record to Bonjour service registration"
```

---

### Task 2: Convert HostModel to Immutable Data Class

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/models/HostModel.kt`

- [ ] **Step 1: Rewrite HostModel as a data class with val fields**

Replace the entire file:

```kotlin
package com.example.ytshare.models

data class HostModel(
    val address: String,
    val hostName: String,
    val port: Int
) {
    override fun toString(): String = "$address:$port"
}
```

Changes: `class` → `data class`, `var` → `val`, removed unused `java.net.InetAddress` import.

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/models/HostModel.kt
git commit -m "refactor: convert HostModel to immutable data class"
```

---

### Task 3: Rewrite NSDHelper with StateFlow and Lifecycle Management

**Files:**
- Rewrite: `YTShare.Android/app/src/main/java/com/example/ytshare/helpers/NSDHelper.kt`

- [ ] **Step 1: Write the new NSDHelper**

Replace the entire file:

```kotlin
package com.example.ytshare.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.example.ytshare.models.HostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.InetAddress

class NSDHelper(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _hosts = MutableStateFlow<List<HostModel>>(emptyList())
    val hosts: StateFlow<List<HostModel>> = _hosts.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (discoveryListener != null) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: $service")
                if (service.serviceType == SERVICE_TYPE) {
                    nsdManager.resolveService(service, createResolveListener())
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                _hosts.update { list ->
                    list.filter { it.hostName != service.serviceName }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Service discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed with error code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed with error code $errorCode")
            }
        }

        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Discovery already stopped", e)
            }
            discoveryListener = null
        }
        _hosts.value = emptyList()
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                Log.d(TAG, "Resolved service: $resolvedService")

                val hostname = resolvedService.attributes["hostname"]
                    ?.decodeToString()
                    ?: resolvedService.serviceName

                val port = resolvedService.port
                val addresses = getHostAddresses(resolvedService)

                val validIpv4 = addresses.filter { it is Inet4Address && isValidIPv4(it.hostAddress) }
                if (validIpv4.isEmpty()) return

                val bestIp = pickBestIp(validIpv4) ?: return
                val host = HostModel(bestIp.hostAddress, hostname, port)

                _hosts.update { list ->
                    val filtered = list.filter { it.hostName != hostname }
                    filtered + host
                }
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed with error code $errorCode")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getHostAddresses(service: NsdServiceInfo): List<InetAddress> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.hostAddresses
        } else {
            listOfNotNull(service.host)
        }
    }

    private fun pickBestIp(candidates: List<InetAddress>): InetAddress? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val deviceSubnet = getDeviceSubnet24() ?: return candidates.first()

        return candidates.firstOrNull { candidate ->
            getSubnet24(candidate.hostAddress) == deviceSubnet
        } ?: candidates.first()
    }

    private fun getDeviceSubnet24(): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val linkProperties: LinkProperties =
            connectivityManager.getLinkProperties(network) ?: return null

        val ipv4Address = linkProperties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }

        return ipv4Address?.let { getSubnet24(it.hostAddress) }
    }

    private fun getSubnet24(ip: String?): String? {
        if (ip == null) return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    companion object {
        private const val TAG = "NSD"
        private const val SERVICE_TYPE = "_http._tcp."

        fun isValidIPv4(ip: String?): Boolean {
            if (ip == null) return false
            val pattern = Regex(
                """^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"""
            )
            return pattern.matches(ip)
        }
    }
}
```

- [ ] **Step 2: Verify the project compiles**

Open the project in Android Studio or run a Gradle check:
```
cd YTShare.Android && ./gradlew compileDebugKotlin
```
Expected: Compilation errors in `MainActivityCompose.kt` referencing `nsd.addresses` and `nsd.discoverServices()` — this is expected and will be fixed in Task 6.

- [ ] **Step 3: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/helpers/NSDHelper.kt
git commit -m "refactor: rewrite NSDHelper with StateFlow, lifecycle, and best-IP selection"
```

---

### Task 4: Create SettingsViewModel

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/SettingsViewModel.kt`

- [ ] **Step 1: Create the SettingsViewModel**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/SettingsViewModel.kt`:

```kotlin
package com.example.ytshare.ui.screens

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.ytshare.Constants
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.models.HostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val nsdHelper: NSDHelper,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    val hosts: StateFlow<List<HostModel>> = nsdHelper.hosts

    private val _isTrackingEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(Constants.isTracking, false)
    )
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    private val _selectedIp = MutableStateFlow(
        sharedPreferences.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0"
    )
    val selectedIp: StateFlow<String> = _selectedIp.asStateFlow()

    fun selectHost(host: HostModel) {
        val hostString = host.toString()
        SharedPrefHelper.saveIp(hostString, sharedPreferences)
        _selectedIp.value = hostString
    }

    fun setTracking(enabled: Boolean) {
        SharedPrefHelper.savePref(enabled, sharedPreferences)
        _isTrackingEnabled.value = enabled
    }
}
```

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/SettingsViewModel.kt
git commit -m "feat: add SettingsViewModel for NSD host selection and tracking"
```

---

### Task 5: Register NSDHelper, SharedPreferences, and SettingsViewModel in Koin

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt`

- [ ] **Step 1: Add imports and new Koin registrations**

Add the following imports at the top of `AppModules.kt` (after existing imports):

```kotlin
import android.content.SharedPreferences
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.ui.screens.SettingsViewModel
```

Add a new module after the existing `viewModelModule`:

```kotlin
val helperModule = module {
    single { NSDHelper(androidContext()) }
    single<SharedPreferences> {
        androidContext().getSharedPreferences("ytshare_prefs", Context.MODE_PRIVATE)
    }
}
```

Add `SettingsViewModel` to the existing `viewModelModule`:

```kotlin
val viewModelModule = module {
    viewModel { HistoryViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { ConversationViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
```

Update the `appModules` list:

```kotlin
val appModules = listOf(databaseModule, networkModule, repositoryModule, viewModelModule, helperModule)
```

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt
git commit -m "feat: register NSDHelper, SharedPreferences, and SettingsViewModel in Koin"
```

---

### Task 6: Update SettingsScreen to Use SettingsViewModel

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Rewrite SettingsScreen to use ViewModel**

Replace the entire file:

```kotlin
package com.example.ytshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytshare.models.HostModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onIpChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val hosts by viewModel.hosts.collectAsState()
    val isTrackingEnabled by viewModel.isTrackingEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Available Devices",
            fontSize = 30.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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

Note: `onIpChanged` callback is kept so `MainActivityCompose` can update the IP state displayed in `HomeScreen`. This is a lightweight bridge — `HomeScreen` still reads IP from the activity-level state.

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/SettingsScreen.kt
git commit -m "refactor: update SettingsScreen to use SettingsViewModel"
```

---

### Task 7: Clean Up MainActivityCompose

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt`

- [ ] **Step 1: Rewrite MainActivityCompose**

Replace the entire file:

```kotlin
package com.example.ytshare

import android.content.Context
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
import com.example.ytshare.data.remote.StompSessionManager
import com.example.ytshare.data.repository.ChatRepository
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.ui.screens.HomeScreen
import com.example.ytshare.ui.screens.SettingsScreen
import com.example.ytshare.ui.screens.auth.AuthViewModel
import com.example.ytshare.ui.screens.auth.LoginScreen
import com.example.ytshare.ui.screens.chat.ConversationScreen
import com.example.ytshare.ui.screens.chat.ConversationViewModel
import com.example.ytshare.ui.screens.chat.FriendsScreen
import com.example.ytshare.ui.screens.chat.FriendsViewModel
import com.example.ytshare.ui.screens.history.HistoryScreen
import com.example.ytshare.ui.screens.history.HistoryViewModel
import com.example.ytshare.ui.theme.YTShareTheme
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
                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

                if (isAuthenticated) {
                    MainScreen(authViewModel)
                } else {
                    LoginScreen(viewModel = authViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nsd.startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        nsd.stopDiscovery()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    fun MainScreen(authViewModel: AuthViewModel) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val stompManager: StompSessionManager = org.koin.compose.koinInject()
        val chatRepo: ChatRepository = org.koin.compose.koinInject()

        LaunchedEffect(Unit) {
            stompManager.connect()
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try { chatRepo.registerDeviceToken(token) } catch (e: Exception) { Log.w("FCM", "Failed to register device token", e) }
                    }
                }
            } catch (e: Exception) { Log.w("FCM", "Failed to get FCM token", e) }
        }

        var ipAddress by remember { mutableStateOf(sharedPref.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0") }
        var savedLink by remember { mutableStateOf(sharedPref.getString(Constants.link, "")) }
        var isTracking by remember { mutableStateOf(sharedPref.getBoolean(Constants.isTracking, false)) }

        // Auto-select first discovered host if no IP is saved
        val hosts by nsd.hosts.collectAsState()
        LaunchedEffect(hosts) {
            if (ipAddress == "0.0.0.0" && hosts.isNotEmpty()) {
                val host = hosts.first()
                SharedPrefHelper.saveIp(host.toString(), sharedPref)
                ipAddress = host.toString()
            }
        }

        Scaffold(
            bottomBar = {
                if (currentRoute?.startsWith("conversation") != true) NavigationBar(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEach { item ->
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
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.White,
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

Key changes:
- `nsd` and `sharedPref` injected via Koin (`by inject()`)
- `db: DBHelper` removed entirely
- `initializeNSD()` removed — replaced by `onResume()`/`onPause()` lifecycle binding
- Auto-select first host via `LaunchedEffect(hosts)` — only if no IP saved yet
- `composable("settings")` now uses `SettingsScreen(onIpChanged = ...)` — no prop drilling of hosts/tracking
- `LaunchedEffect(Unit)` that read `nsd.addresses` removed from settings composable

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt
git commit -m "refactor: clean up MainActivityCompose with Koin injection and NSD lifecycle"
```

---

### Task 8: Remove Unused `db` Parameter from HomeScreen

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HomeScreen.kt:32-39`

- [ ] **Step 1: Remove the `db` parameter from HomeScreen**

In `HomeScreen.kt`, change the composable signature from:

```kotlin
@Composable
fun HomeScreen(
    savedLink: String?,
    ipAddress: String,
    isTracking: Boolean,
    queue: RequestQueue,
    db: com.example.ytshare.helpers.DBHelper,
    onNavigateToSettings: () -> Unit,
    onClearLink: () -> Unit
) {
```

to:

```kotlin
@Composable
fun HomeScreen(
    savedLink: String?,
    ipAddress: String,
    isTracking: Boolean,
    queue: RequestQueue,
    onNavigateToSettings: () -> Unit,
    onClearLink: () -> Unit
) {
```

- [ ] **Step 2: Commit**

```
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HomeScreen.kt
git commit -m "refactor: remove unused DBHelper parameter from HomeScreen"
```

---

### Task 9: Delete Dead Code — HostAdapter

**Files:**
- Delete: `YTShare.Android/app/src/main/java/com/example/ytshare/adapters/HostAdapter.kt`

- [ ] **Step 1: Delete HostAdapter.kt**

```
rm YTShare.Android/app/src/main/java/com/example/ytshare/adapters/HostAdapter.kt
```

This file is a legacy RecyclerView adapter that references `MainActivity` directly. It was replaced by the Compose `HostCard` composable in `SettingsScreen.kt`.

- [ ] **Step 2: Check if the `adapters` directory is now empty and can be removed**

```
ls YTShare.Android/app/src/main/java/com/example/ytshare/adapters/
```

If empty, delete the directory:
```
rmdir YTShare.Android/app/src/main/java/com/example/ytshare/adapters/
```

- [ ] **Step 3: Commit**

```
git add -A YTShare.Android/app/src/main/java/com/example/ytshare/adapters/
git commit -m "refactor: delete dead HostAdapter code replaced by Compose SettingsScreen"
```

---

### Task 10: Build Verification and Smoke Test

- [ ] **Step 1: Build the Android project**

```
cd YTShare.Android && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, fix them before proceeding.

- [ ] **Step 2: Build the host project**

```
cd YTShare.Host/YTShare.Server && dotnet build
```

Expected: Build succeeded.

- [ ] **Step 3: Manual smoke test**

1. Start the host app on the Windows PC — verify the service registers with hostname in logs
2. Install the Android debug APK on a phone on the same LAN
3. Open the Settings screen — verify:
   - Host card shows the PC's computer name (not "Unknown")
   - Only one card per host (not one per network adapter)
4. Tap the host card — verify IP is selected
5. Navigate to Home — verify the selected IP appears in the bar
6. Background the app and re-open — verify discovery restarts and hosts reappear
7. Stop the host app — verify the card disappears from Settings
8. Share a YouTube link — verify it opens on the PC

- [ ] **Step 4: Final commit if any smoke-test fixes were needed**

```
git add -A
git commit -m "fix: smoke test corrections"
```
