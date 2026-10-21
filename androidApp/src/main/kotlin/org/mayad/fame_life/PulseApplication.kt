package org.mayad.fame_life

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.mayad.fame_life.core.platform.PulseAndroid
import org.mayad.fame_life.di.initKoin

/** Process start: platform context and Firebase first, then the dependency graph. */
class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PulseAndroid.init(this, BuildConfig.PULSE_BACKEND_URL, BuildConfig.PULSE_REWARDED_AD_UNIT)
        initKoin { androidContext(this@PulseApplication) }
    }
}
