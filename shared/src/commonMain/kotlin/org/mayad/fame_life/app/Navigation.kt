package org.mayad.fame_life.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mayad.fame_life.domain.simulation.SimulationConfig

enum class Tab { HOME, DISCOVER, INBOX, PROFILE }

@Serializable
sealed interface Route {
    /** Stable key for saved state and transitions. */
    val key: String

    @Serializable data object Main : Route { override val key get() = "main" }
    @Serializable data class PostDetail(val postId: String) : Route { override val key get() = "post/$postId" }
    @Serializable data class CreatorProfile(val creatorId: String) : Route { override val key get() = "creator/$creatorId" }
    @Serializable data class LiveSetup(val title: String = "") : Route { override val key get() = "live-setup" }
    @Serializable data class Live(val sessionId: String, val config: SimulationConfig) : Route { override val key get() = "live/$sessionId" }
    @Serializable data class Summary(val sessionId: String) : Route { override val key get() = "summary/$sessionId" }
    @Serializable data object Settings : Route { override val key get() = "settings" }
    @Serializable data object EditProfile : Route { override val key get() = "edit-profile" }
    @Serializable data object Paywall : Route { override val key get() = "paywall" }
}

/**
 * A small back stack. The app has one level of tabs and a handful of pushed screens, which does not justify a
 * navigation library; this keeps routes typed, saved across configuration changes, and easy to test.
 */
@Stable
class Navigator(initial: List<Route>) {
    val stack = mutableStateListOf<Route>().apply { addAll(initial) }
    var lastWasPop by mutableStateOf(false)
        private set

    val current: Route get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(route: Route) {
        if (stack.lastOrNull()?.key == route.key) return
        lastWasPop = false
        stack.add(route)
    }

    fun pop(): Boolean {
        if (!canPop) return false
        lastWasPop = true
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Replaces the top screen, used for live → summary so back does not return to a finished session. */
    fun replace(route: Route) {
        lastWasPop = false
        stack[stack.lastIndex] = route
    }

    /** Returns to the tabs, dropping everything pushed on top. */
    fun popToMain() {
        lastWasPop = true
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val serializer = ListSerializer(Route.serializer())

        val Saver: Saver<Navigator, String> = Saver(
            save = { json.encodeToString(serializer, it.stack.toList()) },
            restore = { saved ->
                val routes = runCatching { json.decodeFromString(serializer, saved) }.getOrNull()
                // A live session cannot be resumed after the process was recreated; land on Home instead.
                Navigator(routes?.filterNot { it is Route.Live }?.ifEmpty { null } ?: listOf(Route.Main))
            },
        )
    }
}

@Composable
fun rememberNavigator(): Navigator = rememberSaveable(saver = Navigator.Saver) { Navigator(listOf(Route.Main)) }
