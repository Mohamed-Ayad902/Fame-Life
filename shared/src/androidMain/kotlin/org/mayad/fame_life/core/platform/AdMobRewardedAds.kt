package org.mayad.fame_life.core.platform

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mayad.fame_life.domain.economy.AdOutcome
import org.mayad.fame_life.domain.economy.RewardedAds
import kotlin.coroutines.resume

/**
 * AdMob rewarded ads. The ad carries the Firebase uid as its server-side-verification user id; AdMob then calls the
 * backend's `/v1/ads/admob-ssv` with a signed callback, and only that callback credits points. Earning the reward on
 * the device credits nothing by itself.
 *
 * Consent comes first: Google's User Messaging Platform shows the consent form where the law needs one (EEA, UK), and
 * no ad is requested until it says ads may be requested.
 */
internal class AdMobRewardedAds(private val unitId: String) : RewardedAds {
    private var initialized = false

    override val isAvailable: Boolean get() = unitId.isNotBlank()

    override suspend fun show(userId: String): AdOutcome = withContext(Dispatchers.Main) {
        val activity = PulseAndroid.activity ?: return@withContext AdOutcome.NOT_READY
        if (!isAvailable || !gatherConsent(activity)) return@withContext AdOutcome.NOT_READY
        if (!initialized) {
            MobileAds.initialize(activity.applicationContext)
            initialized = true
        }
        val ad = load(activity) ?: return@withContext AdOutcome.FAILED
        ad.setServerSideVerificationOptions(ServerSideVerificationOptions.Builder().setUserId(userId).build())
        present(activity, ad)
    }

    private suspend fun gatherConsent(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (continuation.isActive) continuation.resume(consent.canRequestAds())
                }
            },
            {
                // Offline or misconfigured: a consent given earlier still counts.
                if (continuation.isActive) continuation.resume(consent.canRequestAds())
            },
        )
    }

    private suspend fun load(activity: Activity): RewardedAd? = suspendCancellableCoroutine { continuation ->
        RewardedAd.load(
            activity,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (continuation.isActive) continuation.resume(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    println("Pulse: rewarded ad failed to load (code ${error.code})")
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }

    private suspend fun present(activity: Activity, ad: RewardedAd): AdOutcome = suspendCancellableCoroutine { continuation ->
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (continuation.isActive) continuation.resume(if (earned) AdOutcome.EARNED else AdOutcome.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                println("Pulse: rewarded ad failed to show (code ${error.code})")
                if (continuation.isActive) continuation.resume(AdOutcome.FAILED)
            }
        }
        ad.show(activity) { earned = true }
    }
}
