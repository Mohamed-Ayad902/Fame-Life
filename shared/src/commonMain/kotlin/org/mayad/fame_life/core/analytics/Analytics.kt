package org.mayad.fame_life.core.analytics

/**
 * Typed product analytics. Event names follow Firebase conventions (snake_case, max 40 chars) so the
 * Firebase adapter can forward them unchanged. Parameters never carry transcripts, comment text, names
 * or other personal content.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {
    data object AppOpened : AnalyticsEvent("app_opened")
    data object OnboardingStarted : AnalyticsEvent("onboarding_started")
    data object ProfileSetupCompleted : AnalyticsEvent("profile_setup_completed")
    data class InterestsSelected(val count: Int) : AnalyticsEvent("interests_selected", mapOf("count" to count))
    data object HomeViewed : AnalyticsEvent("home_viewed")
    data object DiscoverViewed : AnalyticsEvent("discover_viewed")
    data class SearchPerformed(val resultCount: Int) : AnalyticsEvent("search_performed", mapOf("result_count" to resultCount))
    data class PostViewed(val category: String) : AnalyticsEvent("post_viewed", mapOf("category" to category))
    data class ProfileViewed(val own: Boolean) : AnalyticsEvent("profile_viewed", mapOf("own" to own))
    data object CreateLiveTapped : AnalyticsEvent("create_live_tapped")
    data object LiveSetupOpened : AnalyticsEvent("live_setup_opened")
    data class LiveStarted(val preset: String, val style: String, val plan: String, val camera: Boolean, val mic: Boolean) :
        AnalyticsEvent("live_started", mapOf("audience_preset" to preset, "live_style" to style, "plan" to plan, "camera" to camera, "mic" to mic))
    data class LiveCompleted(val durationSeconds: Int, val aiRequests: Int, val aiFailures: Int, val fallbacks: Int) :
        AnalyticsEvent("live_completed", mapOf("duration_s" to durationSeconds, "ai_requests" to aiRequests, "ai_failures" to aiFailures, "ai_fallbacks" to fallbacks))
    data class LiveAbandoned(val elapsedSeconds: Int) : AnalyticsEvent("live_abandoned", mapOf("elapsed_s" to elapsedSeconds))
    data object LiveSummaryViewed : AnalyticsEvent("live_summary_viewed")
    data object LiveRestarted : AnalyticsEvent("live_restarted")
    data object SummaryShared : AnalyticsEvent("summary_shared")
    data class AchievementUnlocked(val level: String) : AnalyticsEvent("achievement_unlocked", mapOf("level" to level))
    data class PermissionResult(val camera: Boolean, val mic: Boolean) : AnalyticsEvent("live_permissions", mapOf("camera" to camera, "mic" to mic))
    data object PaywallViewed : AnalyticsEvent("paywall_viewed")
    data object SignupStarted : AnalyticsEvent("signup_started")
    data object SignupCompleted : AnalyticsEvent("sign_up", mapOf("method" to "password"))
    data object SignInCompleted : AnalyticsEvent("login", mapOf("method" to "password"))
    data class AuthFailed(val kind: String) : AnalyticsEvent("auth_failed", mapOf("error_category" to kind))
    data object PasswordResetRequested : AnalyticsEvent("password_reset_requested")
    data object SignedOut : AnalyticsEvent("signed_out")
    data object AccountDeleted : AnalyticsEvent("account_deleted")
}

fun interface Analytics {
    fun log(event: AnalyticsEvent)
}

/** Keeps recent events in memory for debugging and tests until the Firebase adapter is configured. */
class InMemoryAnalytics(private val capacity: Int = 200) : Analytics {
    private val buffer = ArrayDeque<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = buffer.toList()

    override fun log(event: AnalyticsEvent) {
        buffer.addLast(event)
        while (buffer.size > capacity) buffer.removeFirst()
    }
}
