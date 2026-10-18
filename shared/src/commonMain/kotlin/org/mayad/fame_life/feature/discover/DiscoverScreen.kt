package org.mayad.fame_life.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_simulated
import famelife.shared.generated.resources.discover_clear_search
import famelife.shared.generated.resources.discover_creators
import famelife.shared.generated.resources.discover_creators_to_know
import famelife.shared.generated.resources.discover_followers
import famelife.shared.generated.resources.discover_for_you
import famelife.shared.generated.resources.discover_live_experiences
import famelife.shared.generated.resources.discover_most_loved
import famelife.shared.generated.resources.discover_nothing_for
import famelife.shared.generated.resources.discover_posts
import famelife.shared.generated.resources.discover_search_hint
import famelife.shared.generated.resources.discover_search_tip
import famelife.shared.generated.resources.discover_title
import famelife.shared.generated.resources.discover_topic_count
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.SectionHeader
import org.mayad.fame_life.core.designsystem.components.bleed
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.headerScroll
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.motion.scrollLinkedHeader
import org.mayad.fame_life.core.designsystem.preview.ComponentPreviews
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.PulseWidth
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.LiveRoom
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.data.content.Topic
import org.mayad.fame_life.feature.home.LiveRoomCard
import org.mayad.fame_life.feature.home.PostTile
import org.mayad.fame_life.feature.home.glyph

/** Search is logged once the user pauses typing, not on every keystroke. */
private const val SEARCH_SETTLE_MS = 600L

/** What Discover shows for the current query and category. */
@Immutable
data class DiscoverState(
    val query: String,
    val category: Category?,
    val creators: List<Creator>,
    val rooms: List<LiveRoom>,
    val topics: List<Topic>,
    val posts: List<Post>,
    val nothingFound: Boolean,
) {
    val searching: Boolean get() = query.isNotBlank()
}

internal fun discoverState(query: String, category: Category?): DiscoverState {
    val searching = query.isNotBlank()
    val results = SeedContent.search(query)
    return DiscoverState(
        query = query,
        category = category,
        creators = when {
            searching -> results.creators
            category != null -> SeedContent.creators.filter { it.category == category }
            else -> SeedContent.creators.sortedByDescending { it.followers }
        },
        rooms = if (searching) emptyList() else SeedContent.liveRooms.filter { category == null || it.category == category },
        topics = if (searching) results.topics else emptyList(),
        posts = when {
            searching -> results.posts
            category != null -> SeedContent.posts.filter { it.category == category }
            else -> SeedContent.posts.sortedByDescending { it.likes }
        },
        nothingFound = searching && results.isEmpty,
    )
}

/** Tier 1: owns the query and category, logs views and searches, turns taps into navigation. */
@Composable
fun DiscoverScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val category = categoryName?.let { Category.valueOf(it) }
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.DiscoverViewed) }
    val state = remember(query, category) { discoverState(query, category) }
    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect
        delay(SEARCH_SETTLE_MS)
        container.analytics.log(AnalyticsEvent.SearchPerformed(state.creators.size + state.posts.size + state.topics.size))
    }
    DiscoverContent(
        state = state,
        onQuery = { query = it },
        onCategory = { categoryName = it?.name },
        onCreator = { navigator.push(Route.CreatorProfile(it)) },
        onPost = { navigator.push(Route.PostDetail(it)) },
    )
}

/** Tier 2: stateless editorial grid; columns follow the window width. */
@Composable
fun DiscoverContent(
    state: DiscoverState,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
) {
    val dimens = Pulse.dimens
    val grid = rememberLazyStaggeredGridState()
    val cascade = rememberCascadeState()
    val columns = when (Pulse.width) {
        PulseWidth.Compact -> 2
        PulseWidth.Medium -> 3
        PulseWidth.Expanded -> 4
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimens.gutter, end = dimens.gutter, bottom = dimens.bottomBarClearance),
        horizontalArrangement = Arrangement.spacedBy(dimens.md),
        verticalItemSpacing = dimens.lg,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "header") {
            Column(Modifier.scrollLinkedHeader { grid.headerScroll() }) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(dimens.md))
                Text(stringResource(Res.string.discover_title), style = Pulse.type.display, color = Pulse.colors.textPrimary)
                Spacer(Modifier.height(dimens.lg))
                SearchField(state.query, onChange = onQuery)
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "categories") {
            LazyRow(
                modifier = Modifier.bleed(dimens.gutter),
                contentPadding = PaddingValues(horizontal = dimens.gutter),
                horizontalArrangement = Arrangement.spacedBy(dimens.sm),
            ) {
                item(key = "all") {
                    PulseChip(stringResource(Res.string.discover_for_you), selected = state.category == null, onClick = { onCategory(null) })
                }
                items(Category.entries, key = { it.name }) { c ->
                    PulseChip(
                        c.label(),
                        selected = state.category == c,
                        onClick = { onCategory(c) },
                        leading = c.glyph(),
                        modifier = Modifier.cascadeIn(cascade, "cat-${c.name}"),
                    )
                }
            }
        }
        if (state.nothingFound) {
            item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                EmptyState(
                    Glyphs.Search,
                    Gloss.Slate,
                    title = stringResource(Res.string.discover_nothing_for, state.query.trim()),
                    body = stringResource(Res.string.discover_search_tip),
                    action = stringResource(Res.string.discover_clear_search),
                    onAction = { onQuery("") },
                )
            }
        }
        if (state.topics.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "topics") {
                LazyRow(Modifier.bleed(dimens.gutter), contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
                    items(state.topics, key = { it.tag }) { topic ->
                        PulseChip(stringResource(Res.string.discover_topic_count, topic.tag, compactCount(topic.posts)), selected = false, onClick = { onQuery(topic.tag) })
                    }
                }
            }
        }
        if (state.creators.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "creators") {
                Column(Modifier.cascadeIn(cascade, "creators")) {
                    SectionHeader(stringResource(if (state.searching) Res.string.discover_creators else Res.string.discover_creators_to_know))
                    Spacer(Modifier.height(dimens.md))
                    LazyRow(Modifier.bleed(dimens.gutter), contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
                        items(state.creators, key = { it.id }) { creator ->
                            CreatorCard(creator, Modifier.cascadeIn(cascade, "creator-${creator.id}")) { onCreator(creator.id) }
                        }
                    }
                }
            }
        }
        if (state.rooms.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "lives") {
                Column(Modifier.cascadeIn(cascade, "lives")) {
                    SectionHeader(stringResource(Res.string.discover_live_experiences), eyebrow = stringResource(Res.string.common_simulated))
                    Spacer(Modifier.height(dimens.md))
                    LazyRow(Modifier.bleed(dimens.gutter), contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
                        items(state.rooms, key = { it.id }) { room ->
                            LiveRoomCard(room, onOpen = { onCreator(room.creatorId) }, modifier = Modifier.cascadeIn(cascade, "room-${room.id}"))
                        }
                    }
                }
            }
        }
        if (state.posts.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "posts-header") {
                SectionHeader(
                    when {
                        state.searching -> stringResource(Res.string.discover_posts)
                        state.category != null -> state.category.label()
                        else -> stringResource(Res.string.discover_most_loved)
                    },
                )
            }
            itemsIndexed(state.posts, key = { _, post -> "d_${post.id}" }) { index, post ->
                PostTile(post, index + 1, onOpen = { onPost(post.id) }, modifier = Modifier.cascadeIn(cascade, "post-${post.id}"))
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        Modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(Pulse.shapes.control)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.control)
            .padding(horizontal = dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Search, colors.textMuted, size = dimens.icon)
        Spacer(Modifier.width(dimens.md))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(stringResource(Res.string.discover_search_hint), style = Pulse.type.body, color = colors.textMuted)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Pulse.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.cyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            PulseIcon(
                Glyphs.Close,
                colors.textSecondary,
                size = dimens.iconSmall,
                contentDescription = stringResource(Res.string.discover_clear_search),
                modifier = Modifier.pressable { onChange("") },
            )
        }
    }
}

@Composable
private fun CreatorCard(creator: Creator, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Column(
        modifier
            .width(dimens.railCardNarrow)
            .pressable(onClick = onClick)
            .clip(Pulse.shapes.card)
            .background(colors.surface)
            .border(dimens.hairline, colors.border, Pulse.shapes.card)
            .padding(dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CreatorAvatar(creator.avatar, dimens.avatarLg)
        Spacer(Modifier.height(dimens.sm))
        Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("@${creator.handle}", style = Pulse.type.meta, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(dimens.xs))
        Text(stringResource(Res.string.discover_followers, compactCount(creator.followers)), style = Pulse.type.meta, color = colors.textSecondary)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun DiscoverPreview() = PreviewContainer { AtmosphereBackground { DiscoverContent(discoverState("", null), {}, {}, {}, {}) } }

@ScreenPreviews
@Composable
private fun DiscoverEmptySearchPreview() = PreviewContainer { AtmosphereBackground { DiscoverContent(discoverState("zzzz", null), {}, {}, {}, {}) } }

@ComponentPreviews
@Composable
private fun CreatorCardPreview() = PreviewContainer { CreatorCard(SampleData.creator, Modifier.padding(Pulse.dimens.gutter)) {} }
