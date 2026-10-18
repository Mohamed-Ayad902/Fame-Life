package org.mayad.fame_life.data.cloud

import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.domain.simulation.SessionSummary

/** Per-uid documents in memory, shaped like the Firestore layout. Set [offline] to make every call fail. */
class FakeCloudStore : CloudStore {
    val profiles = mutableMapOf<String, CreatorProfile>()
    val sessions = mutableMapOf<String, MutableMap<String, SessionSummary>>()
    val notifications = mutableMapOf<String, MutableMap<String, InAppNotification>>()
    var offline = false
    var writes = 0

    private fun check() {
        if (offline) throw IllegalStateException("offline")
    }

    override suspend fun loadProfile(uid: String): CreatorProfile? = check().let { profiles[uid] }

    override suspend fun saveProfile(uid: String, profile: CreatorProfile) {
        check(); writes++
        profiles[uid] = profile
    }

    override suspend fun loadSessions(uid: String, limit: Int): List<SessionSummary> {
        check()
        return sessions[uid].orEmpty().values.sortedByDescending { it.startedAtEpochMs }.take(limit)
    }

    override suspend fun saveSession(uid: String, summary: SessionSummary) {
        check(); writes++
        sessions.getOrPut(uid) { mutableMapOf() }[summary.id] = summary
    }

    override suspend fun deleteSessions(uid: String, ids: List<String>) {
        check()
        ids.forEach { sessions[uid]?.remove(it) }
    }

    override suspend fun loadNotifications(uid: String, limit: Int): List<InAppNotification> {
        check()
        return notifications[uid].orEmpty().values.sortedByDescending { it.createdAtEpochMs }.take(limit)
    }

    override suspend fun saveNotifications(uid: String, notifications: List<InAppNotification>) {
        check(); writes++
        val docs = this.notifications.getOrPut(uid) { mutableMapOf() }
        notifications.forEach { docs[it.id] = it }
    }

    override suspend fun deleteEverything(uid: String) {
        check()
        profiles.remove(uid)
        sessions.remove(uid)
        notifications.remove(uid)
    }
}
