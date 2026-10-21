package org.mayad.fame_life.core.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.apps
import org.mayad.fame_life.data.remote.PulseBackend
import org.mayad.fame_life.domain.ai.SpeechInput
import org.mayad.fame_life.domain.economy.NoRewardedAds
import org.mayad.fame_life.domain.economy.RewardedAds
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

private class UserDefaultsTextStore(private val defaults: NSUserDefaults) : TextStore {
    override fun read(key: String): String? = defaults.stringForKey(key)
    override fun write(key: String, value: String) = defaults.setObject(value, forKey = key)
    override fun remove(key: String) = defaults.removeObjectForKey(key)
}

actual fun createTextStore(): TextStore = UserDefaultsTextStore(NSUserDefaults.standardUserDefaults)

/** iOSApp.swift calls FirebaseApp.configure() only when GoogleService-Info.plist is bundled. */
actual fun isFirebaseConfigured(): Boolean = Firebase.apps().isNotEmpty()

actual fun isReduceMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()

actual fun shareText(text: String) {
    var top: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    while (true) top = top.presentedViewController ?: break
    val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    // iPad presents the share sheet as a popover, which needs an anchor.
    sheet.popoverPresentationController?.sourceView = top.view
    top.presentViewController(sheet, animated = true, completion = null)
}

/** The iOS HTTP client (URLSession) is not built yet, so iOS lives use the on-device audience. */
actual fun createPulseBackend(): PulseBackend? = null

/** iOS microphone capture is not built yet; the live screen says so and uses the room's vibe. */
actual fun createSpeechInput(backend: PulseBackend?): SpeechInput? = null

actual fun utcOffsetMinutes(epochMs: Long): Int =
    (NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)) / 60).toInt()

/** The Google Mobile Ads SDK isn't linked on iOS yet. */
actual fun createRewardedAds(): RewardedAds = NoRewardedAds

private object UnsupportedLivePermissions : LivePermissions {
    override val camera = false
    override val microphone = false
    override val asked = false
    override val supported = false
    override fun request() = Unit
}

@Composable
actual fun rememberLivePermissions(): LivePermissions = remember { UnsupportedLivePermissions }

/** AVFoundation camera preview is not built yet. */
actual val isCameraPreviewSupported: Boolean = false

@Composable
actual fun CameraPreview(lens: CameraLens, modifier: Modifier) {
    Box(modifier)
}
