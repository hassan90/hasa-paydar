package ir.hasabdari.paydar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ir.hasabdari.paydar.data.local.security.SecurePrefs
import timber.log.Timber

@HiltAndroidApp
class PaydarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // مقداردهی SecurePrefs قبل از هر استفاده
        SecurePrefs.init(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("🚀 حسابداری پایدار راه‌اندازی شد (Secure)")
    }
}