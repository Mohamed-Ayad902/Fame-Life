package org.mayad.fame_life.feature

import org.mayad.fame_life.core.platform.InMemoryTextStore
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository
import org.mayad.fame_life.feature.notifications.InboxIntent
import org.mayad.fame_life.feature.notifications.InboxViewModel
import org.mayad.fame_life.feature.notifications.NotificationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InboxViewModelTest : ViewModelTest() {
    private val hour = 3_600_000L
    private val repo = NotificationRepository(InMemoryTextStore()) { _, _ -> }

    private fun note(id: String, kind: String, at: Long, read: Boolean = false) =
        InAppNotification(id = id, kind = kind, title = "t", body = "b", createdAtEpochMs = at, read = read)

    @Test
    fun openingMarksReadButKeepsTheNewItemsHighlighted() {
        repo.add(note("welcome", NotificationKind.WELCOME, at = 0, read = true))
        repo.add(note("session:s1", NotificationKind.SESSION, at = 0))
        val vm = InboxViewModel(repo) { 3 * hour }

        vm.onIntent(InboxIntent.Opened)

        assertTrue(repo.notifications.value.all { it.read })
        val items = vm.state.value.items.associateBy { it.id }
        assertTrue(items.getValue("session:s1").fresh)
        assertFalse(items.getValue("welcome").fresh)
        assertEquals(3, items.getValue("session:s1").ageHours)
    }

    @Test
    fun onlyRecapsOpenASummary() {
        repo.add(note("session:s1", NotificationKind.SESSION, at = 0))
        repo.add(note("level_up:s2", NotificationKind.LEVEL_UP, at = 0))
        repo.add(note("welcome", NotificationKind.WELCOME, at = 0))
        val items = InboxViewModel(repo) { 0 }.state.value.items.associateBy { it.id }
        assertEquals("s1", items.getValue("session:s1").summaryId)
        assertEquals("s2", items.getValue("level_up:s2").summaryId)
        assertNull(items.getValue("welcome").summaryId)
    }
}
