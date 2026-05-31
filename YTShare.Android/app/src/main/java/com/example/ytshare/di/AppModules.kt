package com.example.ytshare.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.data.auth.AuthRepository
import com.example.ytshare.data.local.AppDatabase
import com.example.ytshare.data.remote.ChatApiService
import com.example.ytshare.data.remote.DeviceTokenApiService
import com.example.ytshare.data.remote.FriendshipApiService
import com.example.ytshare.data.remote.MessageApiService
import com.example.ytshare.data.remote.StompSessionManager
import com.example.ytshare.data.remote.UserApiService
import com.example.ytshare.data.remote.VideoApiService
import com.example.ytshare.data.repository.ChatRepository
import com.example.ytshare.data.repository.VideoRepository
import com.example.ytshare.ui.screens.SettingsViewModel
import com.example.ytshare.ui.screens.auth.AuthViewModel
import com.example.ytshare.ui.screens.chat.ConversationViewModel
import com.example.ytshare.ui.screens.chat.FriendsViewModel
import com.example.ytshare.ui.screens.history.HistoryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ytshare_prefs")

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "ytshare-db"
        ).build()
    }
    single { get<AppDatabase>().videoDao() }
}

val networkModule = module {
    single {
        val authRepository = get<AuthRepository>()
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                val token = runBlocking { authRepository.getIdToken() }
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }
    single { VideoApiService(get()) }
    single { UserApiService(get()) }
    single { FriendshipApiService(get()) }
    single { ChatApiService(get()) }
    single { MessageApiService(get()) }
    single { DeviceTokenApiService(get()) }
    single { StompSessionManager(get()) }
}

val repositoryModule = module {
    single { AuthRepository() }
    single { VideoRepository(get(), get(), androidContext().dataStore) }
    single { ChatRepository(get(), get(), get(), get(), get()) }
}

val viewModelModule = module {
    viewModel { HistoryViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { ConversationViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}

val helperModule = module {
    single { NSDHelper(androidContext()) }
    single<SharedPreferences> {
        androidContext().getSharedPreferences("ytshare_prefs", Context.MODE_PRIVATE)
    }
}

val appModules = listOf(databaseModule, networkModule, repositoryModule, viewModelModule, helperModule)
