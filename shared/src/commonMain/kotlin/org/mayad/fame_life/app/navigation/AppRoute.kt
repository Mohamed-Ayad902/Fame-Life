package org.mayad.fame_life.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.mayad.fame_life.domain.simulation.SimulationConfig

/**
 * Every destination of the signed-in app. Keys are serializable, so the back stack survives configuration changes
 * and process death; arguments travel inside the key.
 */
@Serializable
sealed interface AppRoute : NavKey {
    /** The tab shell (home, discover, inbox, profile). */
    @Serializable data object Main : AppRoute
    @Serializable data class PostDetail(val postId: String) : AppRoute
    @Serializable data class CreatorProfile(val creatorId: String) : AppRoute
    @Serializable data class LiveSetup(val title: String = "") : AppRoute
    @Serializable data class Live(val sessionId: String, val config: SimulationConfig) : AppRoute
    @Serializable data class Summary(val sessionId: String) : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object EditProfile : AppRoute
    @Serializable data object Paywall : AppRoute
}

/** Tells the saved-state serializer about every [AppRoute], which non-JVM platforms need for polymorphic keys. */
internal val AppRouteSavedState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Main::class)
            subclass(AppRoute.PostDetail::class)
            subclass(AppRoute.CreatorProfile::class)
            subclass(AppRoute.LiveSetup::class)
            subclass(AppRoute.Live::class)
            subclass(AppRoute.Summary::class)
            subclass(AppRoute.Settings::class)
            subclass(AppRoute.EditProfile::class)
            subclass(AppRoute.Paywall::class)
        }
    }
}
