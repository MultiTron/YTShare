package com.example.ytshare

import android.app.Application
import com.example.ytshare.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YTShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YTShareApplication)
            modules(appModules)
        }
    }
}
