package com.example.ytshare.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.ytshare.data.auth.AuthRepository
import com.example.ytshare.data.local.AppDatabase
import com.example.ytshare.data.remote.VideoApiService
import com.example.ytshare.data.repository.VideoRepository
import com.example.ytshare.ui.screens.auth.AuthViewModel
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
}

val repositoryModule = module {
    single { AuthRepository() }
    single { VideoRepository(get(), get(), androidContext().dataStore) }
}

val viewModelModule = module {
    viewModel { HistoryViewModel(get()) }
    viewModel { AuthViewModel(get()) }
}

val appModules = listOf(databaseModule, networkModule, repositoryModule, viewModelModule)
