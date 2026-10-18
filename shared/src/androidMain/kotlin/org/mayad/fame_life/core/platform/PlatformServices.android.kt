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
import org.mayad.fame_life.domain.ai.SpeechInput

/** Holds the application context for platform services. Call [init] from the Activity before composing. */
@SuppressLint("StaticFieldLeak") // Application context only, which lives as long as the process.
object PulseAndroid {
    internal lateinit var appContext: Context
        private set

    // The account picker needs an Activity; held weakly so a destroyed Activity is never leaked.
    private var activityRef: WeakReference<Activity>? = null
    internal val activity: Activity? get() = activityRef?.get()

    fun init(activity: Activity) {
        activityRef = WeakReference(activity)
        appContext = activity.applicationContext
        // Firebase's content provider normally does this from google-services.json; calling it again is a no-op,
        // and it makes start-up order explicit. Returns null (and accounts stay unavailable) without that file.
        if (Firebase.apps(appContext).isEmpty()) Firebase.initialize(appContext)
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

actual fun createSpeechInput(): SpeechInput? = AndroidSpeechInput(PulseAndroid.appContext)
