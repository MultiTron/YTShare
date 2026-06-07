package iliev.yt.share.mobile

import android.app.Application
import iliev.yt.share.mobile.di.appModules
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
