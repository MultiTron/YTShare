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
import iliev.yt.share.mobile.helpers.NSDHelper
import iliev.yt.share.mobile.helpers.SharedPrefHelper
import iliev.yt.share.mobile.ui.screens.HomeScreen
import iliev.yt.share.mobile.ui.screens.SettingsScreen
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
