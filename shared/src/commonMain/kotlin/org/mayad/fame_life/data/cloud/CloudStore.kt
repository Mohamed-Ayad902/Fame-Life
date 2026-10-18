package org.mayad.fame_life.data.cloud

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import org.mayad.fame_life.data.local.CreatorProfile
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.domain.simulation.SessionSummary

/**
 * A user's documents in the cloud. Everything is scoped by uid and the server rules (firestore.rules) only let a
 * signed-in user touch their own `users/{uid}` tree. Entitlements and usage counters are deliberately absent:
 * the client never writes them.
 */
interface CloudStore {
    suspend fun loadProfile(uid: String): CreatorProfile?
    suspend fun saveProfile(uid: String, profile: CreatorProfile)

    /** Newest first. */
    suspend fun loadSessions(uid: String, limit: Int): List<SessionSummary>
    suspend fun saveSession(uid: String, summary: SessionSummary)
    suspend fun deleteSessions(uid: String, ids: List<String>)

    /** Newest first. */
    suspend fun loadNotifications(uid: String, limit: Int): List<InAppNotification>
    suspend fun saveNotifications(uid: String, notifications: List<InAppNotification>)

    /** Removes the profile and every session and notification. Used by account deletion. */
    suspend fun deleteEverything(uid: String)
}

/**
 * Firestore layout:
 * - `users/{uid}`: [CreatorProfile]
 * - `users/{uid}/sessions/{sessionId}`: [SessionSummary], read newest first by `startedAtEpochMs`
 * - `users/{uid}/notifications/{id}`: [InAppNotification], read newest first by `createdAtEpochMs`
 *
 * Only finished session summaries are written, once per live; nothing is written while a live is running.
 */
class FirestoreCloudStore : CloudStore {
    private val db = Firebase.firestore

    private fun user(uid: String) = db.collection(USERS).document(uid)
    private fun sessions(uid: String) = user(uid).collection(SESSIONS)
    private fun notifications(uid: String) = user(uid).collection(NOTIFICATIONS)

    override suspend fun loadProfile(uid: String): CreatorProfile? {
        val snapshot = user(uid).get()
        return if (snapshot.exists) snapshot.data(CreatorProfile.serializer()) else null
    }

    override suspend fun saveProfile(uid: String, profile: CreatorProfile) {
        user(uid).set(CreatorProfile.serializer(), profile) { encodeDefaults = true }
    }

    override suspend fun loadSessions(uid: String, limit: Int): List<SessionSummary> =
        sessions(uid).orderBy("startedAtEpochMs", Direction.DESCENDING).limit(limit).get()
            .documents.map { it.data(SessionSummary.serializer()) }

    override suspend fun saveSession(uid: String, summary: SessionSummary) {
        sessions(uid).document(summary.id).set(SessionSummary.serializer(), summary) { encodeDefaults = true }
    }

    override suspend fun deleteSessions(uid: String, ids: List<String>) {
        if (ids.isEmpty()) return
        ids.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(sessions(uid).document(it)) }
            batch.commit()
        }
    }

    override suspend fun loadNotifications(uid: String, limit: Int): List<InAppNotification> =
        notifications(uid).orderBy("createdAtEpochMs", Direction.DESCENDING).limit(limit).get()
            .documents.map { it.data(InAppNotification.serializer()) }

    override suspend fun saveNotifications(uid: String, notifications: List<InAppNotification>) {
        notifications.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(notifications(uid).document(it.id), InAppNotification.serializer(), it) { encodeDefaults = true } }
            batch.commit()
        }
    }

    override suspend fun deleteEverything(uid: String) {
        for (collection in listOf(sessions(uid), notifications(uid))) {
            while (true) {
                val docs = collection.limit(BATCH).get().documents
                if (docs.isEmpty()) break
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit()
            }
        }
        user(uid).delete()
    }

    private companion object {
        const val USERS = "users"
        const val SESSIONS = "sessions"
        const val NOTIFICATIONS = "notifications"
        const val BATCH = 200
    }
}
