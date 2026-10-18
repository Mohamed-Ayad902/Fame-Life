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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.PulseChip
import org.mayad.fame_life.core.designsystem.components.SectionHeader
import org.mayad.fame_life.core.designsystem.components.bleed
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.feature.home.LiveRoomCard
import org.mayad.fame_life.feature.home.PostTile
import org.mayad.fame_life.feature.home.glyph
import org.mayad.fame_life.feature.shell.BottomBarClearance

@Composable
fun DiscoverScreen(navigator: Navigator) {
    val container = LocalAppContainer.current
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val category = categoryName?.let { Category.valueOf(it) }
    LaunchedEffect(Unit) { container.analytics.log(AnalyticsEvent.DiscoverViewed) }

    val results = remember(query) { SeedContent.search(query) }
    val searching = query.isNotBlank()
    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect
        delay(600)
        val r = SeedContent.search(query)
        container.analytics.log(AnalyticsEvent.SearchPerformed(r.creators.size + r.posts.size + r.topics.size))
    }
    val posts = when {
        searching -> results.posts
        category != null -> SeedContent.posts.filter { it.category == category }
        else -> SeedContent.posts.sortedByDescending { it.likes }
    }
    val creators = when {
        searching -> results.creators
        category != null -> SeedContent.creators.filter { it.category == category }
        else -> SeedContent.creators.sortedByDescending { it.followers }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Pulse.dimens.gutter, end = Pulse.dimens.gutter, bottom = BottomBarClearance.calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md),
        verticalItemSpacing = Pulse.dimens.lg,
    ) {
        item(span = StaggeredGridItemSpan.FullLine, key = "header") {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(Pulse.dimens.md))
                Text("Discover", style = Pulse.type.display, color = Pulse.colors.textPrimary)
                Spacer(Modifier.height(Pulse.dimens.lg))
                SearchField(query, onChange = { query = it })
            }
        }
        item(span = StaggeredGridItemSpan.FullLine, key = "categories") {
            LazyRow(
                modifier = Modifier.bleed(Pulse.dimens.gutter),
                contentPadding = PaddingValues(horizontal = Pulse.dimens.gutter),
                horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm),
            ) {
                item(key = "all") { PulseChip("For you", selected = category == null, onClick = { categoryName = null }) }
                items(Category.entries, key = { it.name }) { c ->
                    PulseChip(c.label, selected = category == c, onClick = { categoryName = c.name }, leading = c.glyph())
                }
            }
        }
        if (searching && results.isEmpty) {
            item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                EmptyState(
                    Glyphs.Search,
                    Gloss.Slate,
                    title = "Nothing for \"${query.trim()}\"",
                    body = "Try a creator's name, a category like music, or a tag like #storytime.",
                    action = "Clear search",
                    onAction = { query = "" },
                )
            }
        }
        if (searching && results.topics.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "topics") {
                LazyRow(Modifier.bleed(Pulse.dimens.gutter), contentPadding = PaddingValues(horizontal = Pulse.dimens.gutter), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.sm)) {
                    items(results.topics, key = { it.tag }) { topic ->
                        PulseChip("#${topic.tag} · ${compactCount(topic.posts)}", selected = false, onClick = { query = topic.tag })
                    }
                }
            }
        }
        if (creators.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "creators") {
                Column {
                    SectionHeader(if (searching) "Creators" else "Creators to know")
                    Spacer(Modifier.height(Pulse.dimens.md))
                    LazyRow(Modifier.bleed(Pulse.dimens.gutter), contentPadding = PaddingValues(horizontal = Pulse.dimens.gutter), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
                        items(creators, key = { it.id }) { creator ->
                            CreatorCard(creator) { navigator.push(Route.CreatorProfile(creator.id)) }
                        }
                    }
                }
            }
        }
        if (!searching) {
            val rooms = SeedContent.liveRooms.filter { category == null || it.category == category }
            if (rooms.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine, key = "lives") {
                    Column {
                        SectionHeader("Live experiences", eyebrow = "Simulated")
                        Spacer(Modifier.height(Pulse.dimens.md))
                        LazyRow(Modifier.bleed(Pulse.dimens.gutter), contentPadding = PaddingValues(horizontal = Pulse.dimens.gutter), horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md)) {
                            items(rooms, key = { it.id }) { room ->
                                LiveRoomCard(room, onOpen = { navigator.push(Route.CreatorProfile(room.creatorId)) })
                            }
                        }
                    }
                }
            }
        }
        if (posts.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "posts-header") {
                SectionHeader(if (searching) "Posts" else category?.label ?: "Most loved this week")
            }
            itemsIndexed(posts, key = { _, post -> "d_${post.id}" }) { index, post ->
                PostTile(post, index + 1, onOpen = { navigator.push(Route.PostDetail(post.id)) })
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val colors = Pulse.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(Pulse.shapes.control)
            .background(colors.surface)
            .border(1.dp, colors.border, Pulse.shapes.control)
            .padding(horizontal = Pulse.dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(Glyphs.Search, colors.textMuted, size = 20.dp)
        Spacer(Modifier.width(Pulse.dimens.md))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search creators, posts, #tags", style = Pulse.type.body, color = colors.textMuted)
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
                size = 18.dp,
                contentDescription = "Clear search",
                modifier = Modifier.pressable { onChange("") },
            )
        }
    }
}

@Composable
private fun CreatorCard(creator: Creator, onClick: () -> Unit) {
    val colors = Pulse.colors
    Column(
        Modifier
            .width(132.dp)
            .pressable(onClick = onClick)
            .clip(Pulse.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, Pulse.shapes.card)
            .padding(Pulse.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CreatorAvatar(creator.avatar, 64.dp)
        Spacer(Modifier.height(Pulse.dimens.sm))
        Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("@${creator.handle}", style = Pulse.type.meta, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Pulse.dimens.xs))
        Text("${compactCount(creator.followers)} followers", style = Pulse.type.meta, color = colors.textSecondary)
    }
}
