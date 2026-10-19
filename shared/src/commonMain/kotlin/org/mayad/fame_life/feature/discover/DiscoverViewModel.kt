package org.mayad.fame_life.feature.discover

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mayad.fame_life.core.analytics.Analytics
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.presentation.MviViewModel
import org.mayad.fame_life.core.presentation.NoEvent
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.content.Topic

/** Search is logged once the user pauses typing, not on every keystroke. */
private const val SEARCH_SETTLE_MS = 600L

/** What Discover shows for the current query and category. */
@Immutable
data class DiscoverUiState(
    val query: String,
    val category: Category?,
    val creators: ImmutableList<Creator>,
    val rooms: ImmutableList<LiveRoom>,
    val topics: ImmutableList<Topic>,
    val posts: ImmutableList<Post>,
    val nothingFound: Boolean,
) {
    val searching: Boolean get() = query.isNotBlank()

    companion object {
        /** Builds the results for a query and category from the seed content. */
        fun from(query: String, category: Category?): DiscoverUiState {
            val searching = query.isNotBlank()
            val results = SeedContent.search(query)
            return DiscoverUiState(
                query = query,
                category = category,
                creators = when {
                    searching -> results.creators
                    category != null -> SeedContent.creators.filter { it.category == category }
                    else -> SeedContent.creators.sortedByDescending { it.followers }
                }.toImmutableList(),
                rooms = if (searching) persistentListOf() else SeedContent.liveRooms.filter { category == null || it.category == category }.toImmutableList(),
                topics = (if (searching) results.topics else emptyList()).toImmutableList(),
                posts = when {
                    searching -> results.posts
                    category != null -> SeedContent.posts.filter { it.category == category }
                    else -> SeedContent.posts.sortedByDescending { it.likes }
                }.toImmutableList(),
                nothingFound = searching && results.isEmpty,
            )
        }
    }
}

sealed interface DiscoverIntent {
    data class Query(val value: String) : DiscoverIntent
    data class SelectCategory(val category: Category?) : DiscoverIntent
}

/** Search and category filtering over the seed content. */
class DiscoverViewModel(private val analytics: Analytics) :
    MviViewModel<DiscoverUiState, DiscoverIntent, NoEvent>(DiscoverUiState.from("", null)) {

    private var searchLog: Job? = null

    init {
        analytics.log(AnalyticsEvent.DiscoverViewed)
    }

    override fun onIntent(intent: DiscoverIntent) {
        when (intent) {
            is DiscoverIntent.Query -> {
                setState { DiscoverUiState.from(intent.value, category) }
                logSearchWhenSettled()
            }
            is DiscoverIntent.SelectCategory -> setState { DiscoverUiState.from(query, intent.category) }
        }
    }

    private fun logSearchWhenSettled() {
        searchLog?.cancel()
        if (!currentState.searching) return
        searchLog = viewModelScope.launch {
            delay(SEARCH_SETTLE_MS)
            val s = currentState
            analytics.log(AnalyticsEvent.SearchPerformed(s.creators.size + s.posts.size + s.topics.size))
        }
    }
}
