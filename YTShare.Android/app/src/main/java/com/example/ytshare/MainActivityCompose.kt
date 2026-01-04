package com.example.ytshare

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.ytshare.helpers.DBHelper
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.ui.screens.HistoryScreen
import com.example.ytshare.ui.screens.HomeScreen
import com.example.ytshare.ui.screens.SettingsScreen
import com.example.ytshare.ui.theme.YTShareTheme

class MainActivityCompose : ComponentActivity() {
    
    lateinit var queue: RequestQueue
    lateinit var sharedPref: SharedPreferences
    lateinit var db: DBHelper
    lateinit var nsd: NSDHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize helpers
        initializeNSD()
        db = DBHelper(this, null)
        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)

        // Handle shared link
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }

        setContent {
            YTShareTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // State for preferences
        var ipAddress by remember { mutableStateOf(sharedPref.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0") }
        var savedLink by remember { mutableStateOf(sharedPref.getString(Constants.link, "")) }
        var isTracking by remember { mutableStateOf(sharedPref.getBoolean(Constants.isTracking, false)) }
        var isHistoryDesc by remember { mutableStateOf(sharedPref.getBoolean(Constants.isHistoryDesc, false)) }
        var hosts by remember { mutableStateOf(nsd.addresses) }
        var videos by remember { mutableStateOf(db.getAllLinks()) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    item.icon, 
                                    contentDescription = item.label,
                                    modifier = Modifier.size(androidx.compose.ui.unit.Dp(30f))
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
                        db = db,
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
                    // Refresh videos when navigating to history
                    LaunchedEffect(Unit) {
                        videos = db.getAllLinks()
                    }
                    
                    HistoryScreen(
                        videos = videos,
                        isDescending = isHistoryDesc,
                        onDeleteAll = {
                            db.deleteAll()
                            videos = emptyList()
                        },
                        onSortChanged = { desc ->
                            isHistoryDesc = desc
                            SharedPrefHelper.saveSort(desc, sharedPref)
                        }
                    )
                }

                composable("settings") {
                    // Refresh hosts when navigating to settings
                    LaunchedEffect(Unit) {
                        hosts = nsd.addresses
                    }
                    
                    SettingsScreen(
                        hosts = hosts,
                        isTrackingEnabled = isTracking,
                        onHostSelected = { host ->
                            val hostString = host.toString()
                            SharedPrefHelper.saveIp(hostString, sharedPref)
                            ipAddress = hostString
                        },
                        onTrackingChanged = { enabled ->
                            isTracking = enabled
                            SharedPrefHelper.savePref(enabled, sharedPref)
                        }
                    )
                }
            }
        }
    }

    private fun initializeNSD() {
        nsd = NSDHelper(this)
        nsd.discoverServices()
        
        if (nsd.addresses.isNotEmpty()) {
            val host = nsd.addresses.first()
            if (host.address.isNotEmpty()) {
                SharedPrefHelper.saveIp(host.toString(), sharedPref)
            } else {
                SharedPrefHelper.clearIp(sharedPref)
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
    BottomNavItem("settings", Icons.Filled.Settings, "Settings")
)
