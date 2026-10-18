package org.mayad.fame_life.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import org.mayad.fame_life.core.platform.TextStore
import org.mayad.fame_life.domain.entitlement.Entitlements
import org.mayad.fame_life.domain.progression.FameProgression
import org.mayad.fame_life.domain.simulation.SessionSummary
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The signed-in creator's profile, stored at `users/{uid}` and cached on the device. [famePoints] is a progression
 * total for fun, never used for anything paid.
 */
@Serializable
data class CreatorProfile(
    val displayName: String,
    val username: String,
    val bio: String = "",
    val avatarPalette: Int = 0,
    val interests: List<String> = emptyList(),
    val createdAtEpochMs: Long,
    val famePoints: Int = 0,
    val sessionsCompleted: Int = 0,
)

@Serializable
data class InAppNotification(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val read: Boolean = false,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** A stored value with a hot stream; corrupt data is reported and reset rather than crashing the app. */
private class StoredValue<T>(
    private val store: TextStore,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val onCorrupt: (String, Throwable) -> Unit,
) {
    private val state = MutableStateFlow(load())
    val flow: StateFlow<T> = state.asStateFlow()
    val value: T get() = state.value

    fun set(value: T) {
        store.write(key, json.encodeToString(serializer, value))
        state.value = value
    }

    fun clear() {
        store.remove(key)
        state.value = default
    }

    private fun load(): T {
        val raw = store.read(key) ?: return default
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            onCorrupt(key, e)
            store.remove(key)
            default
        }
    }
}

@OptIn(ExperimentalTime::class)
fun nowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Receives every user-made change so it can be mirrored to the cloud (see `AccountSync`). The repositories below
 * are the device cache that screens read from; [CloudSink.None] keeps them local-only, which tests use.
 */
interface CloudSink {
    fun profileChanged(profile: CreatorProfile) {}
    fun sessionSaved(summary: SessionSummary, removedIds: List<String>) {}
    fun notificationsChanged(changed: List<InAppNotification>) {}

    object None : CloudSink
}

class ProfileRepository(store: TextStore, private val sink: CloudSink = CloudSink.None, onCorrupt: (String, Throwable) -> Unit) {
    private val stored = StoredValue(store, "profile.v1", CreatorProfile.serializer().nullable, null, onCorrupt)
    val profile: StateFlow<CreatorProfile?> = stored.flow

    fun save(profile: CreatorProfile) {
        stored.set(profile)
        sink.profileChanged(profile)
    }

    /** Replaces the cache with what the cloud has, without echoing it back. */
    fun replace(profile: CreatorProfile?) = if (profile == null) stored.clear() else stored.set(profile)

    /** Adds a session's points and returns the level change, if any, so the caller can celebrate it. */
    fun recordSession(points: Int): Pair<Int, Int>? {
        val current = stored.value ?: return null
        val updated = current.copy(famePoints = current.famePoints + points, sessionsCompleted = current.sessionsCompleted + 1)
        stored.set(updated)
        sink.profileChanged(updated)
        val before = FameProgression.levelFor(current.famePoints).ordinal
        val after = FameProgression.levelFor(updated.famePoints).ordinal
        return if (after > before) before to after else null
    }

    fun clear() = stored.clear()
}

class SessionHistoryRepository(store: TextStore, private val sink: CloudSink = CloudSink.None, onCorrupt: (String, Throwable) -> Unit) {
    private val stored = StoredValue(store, "sessions.v1", ListSerializer(SessionSummary.serializer()), emptyList(), onCorrupt)
    val sessions: StateFlow<List<SessionSummary>> = stored.flow

    /** Saves a finished session. Idempotent by id, newest first, trimmed to the plan's history size. */
    fun save(summary: SessionSummary, keep: Int) {
        val merged = listOf(summary) + stored.value.filterNot { it.id == summary.id }
        val updated = merged.take(keep)
        stored.set(updated)
        sink.sessionSaved(summary, removedIds = merged.drop(keep).map { it.id })
    }

    fun replace(sessions: List<SessionSummary>) = stored.set(sessions)

    fun byId(id: String): SessionSummary? = stored.value.firstOrNull { it.id == id }

    fun clear() = stored.clear()
}

class NotificationRepository(store: TextStore, private val sink: CloudSink = CloudSink.None, onCorrupt: (String, Throwable) -> Unit) {
    private val stored = StoredValue(store, "notifications.v1", ListSerializer(InAppNotification.serializer()), emptyList(), onCorrupt)
    val notifications: StateFlow<List<InAppNotification>> = stored.flow

    fun add(notification: InAppNotification) {
        if (stored.value.any { it.id == notification.id }) return
        stored.set((listOf(notification) + stored.value).take(MAX))
        sink.notificationsChanged(listOf(notification))
    }

    fun markAllRead() {
        val changed = stored.value.filterNot { it.read }.map { it.copy(read = true) }
        if (changed.isEmpty()) return
        stored.set(stored.value.map { it.copy(read = true) })
        sink.notificationsChanged(changed)
    }

    fun replace(notifications: List<InAppNotification>) = stored.set(notifications.take(MAX))

    fun clear() = stored.clear()

    companion object {
        const val MAX = 100
    }
}

/**
 * The current plan. Billing is not wired yet, so every user is on Free from [Entitlements.Free]; the paid
 * path must come from a verified server entitlement (written only by the backend to `entitlements/{uid}`),
 * never from a local flag or a client-written document.
 */
class EntitlementRepository {
    private val state = MutableStateFlow(Entitlements.Free)
    val entitlements: StateFlow<Entitlements> = state.asStateFlow()
}
