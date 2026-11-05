package om.yousef.shashati

import android.app.Application
import om.yousef.shashati.data.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
