package org.mayad.fame_life.feature.notifications

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.platform.EpochClock
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.local.InAppNotification
import org.mayad.fame_life.data.local.NotificationRepository

private const val MS_PER_HOUR = 3_600_000L

/**
 * One row, ready to draw.
 * [fresh] stays true for items that were unread when the inbox was opened, so they remain highlighted while read.
 * [summaryId] is the live recap the row opens, or null when it opens nothing.
 */
@Immutable
data class InboxItem(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val fresh: Boolean,
    val ageHours: Int,
    val summaryId: String?,
)

@Immutable
data class InboxUiState(val items: ImmutableList<InboxItem> = persistentListOf())

sealed interface InboxIntent {
    /** The tab became visible: remember what was unread, then mark everything read. */
    data object Opened : InboxIntent
}

class InboxViewModel(
    private val notifications: NotificationRepository,
    private val clock: EpochClock,
) : MviViewModel<InboxUiState, InboxIntent, NoEvent>(InboxUiState()) {

    private var freshIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            notifications.notifications.collect { list -> setState { copy(items = toItems(list)) } }
        }
    }

    override fun onIntent(intent: InboxIntent) {
        when (intent) {
            InboxIntent.Opened -> {
                freshIds = notifications.notifications.value.filterNot { it.read }.mapTo(HashSet()) { it.id }
                setState { copy(items = toItems(notifications.notifications.value)) }
                notifications.markAllRead()
            }
        }
    }

    private fun toItems(list: List<InAppNotification>): ImmutableList<InboxItem> {
        val now = clock.now()
        return list.map { item ->
            InboxItem(
                id = item.id,
                kind = item.kind,
                title = item.title,
                body = item.body,
                fresh = item.id in freshIds,
                ageHours = ((now - item.createdAtEpochMs).coerceAtLeast(0) / MS_PER_HOUR).toInt(),
                summaryId = summaryIdOf(item),
            )
        }.toImmutableList()
    }

    companion object {
        /** Session recaps carry the session id after the colon, e.g. "session:abc". Other kinds open nothing. */
        fun summaryIdOf(item: InAppNotification): String? = when (item.kind) {
            NotificationKind.SESSION, NotificationKind.LEVEL_UP -> item.id.substringAfter(':', "").takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
