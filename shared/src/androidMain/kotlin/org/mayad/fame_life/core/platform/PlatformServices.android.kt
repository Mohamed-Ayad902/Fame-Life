package org.mayad.fame_life.core.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize
import org.mayad.fame_life.data.remote.PulseBackend
import org.mayad.fame_life.data.remote.firebaseIdToken
import org.mayad.fame_life.data.remote.transcribe
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.economy.NoRewardedAds
import org.mayad.fame_life.domain.economy.RewardedAds
import java.util.TimeZone

/**
 * Platform context for the shared code. [init] runs once per process from the Application; [attach] runs from the
 * Activity, because the Google account picker needs one.
 */
@SuppressLint("StaticFieldLeak") // Application context only, which lives as long as the process.
object PulseAndroid {
    internal lateinit var appContext: Context
        private set

    /** The backend base URL from the app's build config; blank when none is configured. */
    internal var backendUrl: String = ""
        private set

    /** The AdMob rewarded ad unit from the app's build config; blank turns reward ads off. */
    internal var rewardedAdUnitId: String = ""
        private set

    // Held weakly so a destroyed Activity is never leaked.
    private var activityRef: WeakReference<Activity>? = null
    internal val activity: Activity? get() = activityRef?.get()

    fun init(context: Context, backendUrl: String = "", rewardedAdUnitId: String = "") {
        appContext = context.applicationContext
        this.backendUrl = backendUrl
        this.rewardedAdUnitId = rewardedAdUnitId
        // Firebase's content provider normally does this from google-services.json; calling it again is a no-op,
        // and it makes start-up order explicit. Returns null (and accounts stay unavailable) without that file.
        if (Firebase.apps(appContext).isEmpty()) Firebase.initialize(appContext)
    }

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }
}

private class PreferencesTextStore(private val prefs: SharedPreferences) : TextStore {
    override fun read(key: String): String? = prefs.getString(key, null)
    override fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

actual fun createTextStore(): TextStore =
    PreferencesTextStore(PulseAndroid.appContext.getSharedPreferences("pulse.local", Context.MODE_PRIVATE))

actual fun isFirebaseConfigured(): Boolean = Firebase.apps(PulseAndroid.appContext).isNotEmpty()

actual fun isReduceMotionEnabled(): Boolean =
    Settings.Global.getFloat(PulseAndroid.appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

actual fun shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    PulseAndroid.appContext.startActivity(chooser)
}

actual fun createPulseBackend(): PulseBackend? =
    PulseAndroid.backendUrl.takeIf { it.startsWith("https://") }?.let { HttpPulseBackend(it, ::firebaseIdToken) }

actual fun createSpeechInput(backend: PulseBackend?): SpeechInput? =
    if (backend != null) CloudSpeechInput(PulseAndroid.appContext) { samples -> backend.transcribe(samples) }
    else AndroidSpeechInput(PulseAndroid.appContext)

actual fun utcOffsetMinutes(epochMs: Long): Int = TimeZone.getDefault().getOffset(epochMs) / 60_000

actual fun createRewardedAds(): RewardedAds =
    PulseAndroid.rewardedAdUnitId.takeIf { it.isNotBlank() }?.let(::AdMobRewardedAds) ?: NoRewardedAds
