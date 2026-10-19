package org.mayad.fame_life.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_back
import famelife.shared.generated.resources.profile_followers
import famelife.shared.generated.resources.profile_level
import famelife.shared.generated.resources.profile_likes
import famelife.shared.generated.resources.profile_live_now
import famelife.shared.generated.resources.profile_posts
import famelife.shared.generated.resources.profile_simulated_creator
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.designsystem.components.AvatarRing
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.LiveBadge
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.StatColumn
import org.mayad.fame_life.core.designsystem.components.ViewerPill
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.headerScroll
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.label
import org.mayad.fame_life.core.ui.title
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.feature.home.PostTile

private const val CREATOR_POSTS_PER_ROW = 2

/** Tier 2: stateless. Wide windows put the identity beside a wider post grid. */
@Composable
fun CreatorProfileContent(state: CreatorProfileUiState, onBack: () -> Unit, onPost: (String) -> Unit) {
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    val dimens = Pulse.dimens
    val cover: @Composable () -> Unit = {
        ProfileCover(
            state.creator.cover,
            state.creator.avatar,
            scroll = { list.headerScroll() },
            ring = if (state.live != null) AvatarRing.LIVE else AvatarRing.STORY,
        )
    }
    Box(Modifier.fillMaxSize().background(Pulse.colors.background)) {
        if (Pulse.width.split) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    cover()
                    CreatorIdentity(state, cascade)
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1.2f).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars),
                    contentPadding = PaddingValues(top = dimens.lg),
                ) { creatorPosts(state.posts, CREATOR_POSTS_PER_ROW + 1, cascade, onPost) }
            }
        } else {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                item(key = "cover") { cover() }
                item(key = "identity") { CreatorIdentity(state, cascade) }
                creatorPosts(state.posts, CREATOR_POSTS_PER_ROW, cascade, onPost)
            }
        }
        GlassIconButton(
            Glyphs.ChevronLeft,
            contentDescription = stringResource(Res.string.common_back),
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(dimens.lg),
        )
    }
}

@Composable
private fun CreatorIdentity(state: CreatorProfileUiState, cascade: CascadeState) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val creator = state.creator
    Column(Modifier.padding(horizontal = dimens.gutter)) {
        Text(creator.displayName, style = Pulse.type.display, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("@${creator.handle} · ${creator.category.label()}", style = Pulse.type.label, color = colors.textSecondary, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(dimens.sm))
            SimulatedTag(text = stringResource(Res.string.profile_simulated_creator))
        }
        Spacer(Modifier.height(dimens.md))
        Text(creator.bio, style = Pulse.type.body, color = colors.textSecondary)
        Spacer(Modifier.height(dimens.lg))
        Row(Modifier.fillMaxWidth().cascadeIn(cascade, "stats"), horizontalArrangement = Arrangement.SpaceBetween) {
            StatColumn(compactCount(creator.followers), stringResource(Res.string.profile_followers), align = Alignment.Start)
            StatColumn(compactCount(creator.totalLikes), stringResource(Res.string.profile_likes))
            StatColumn(creator.level.title(), stringResource(Res.string.profile_level), align = Alignment.End)
        }
        val live = state.live
        if (live != null) {
            Spacer(Modifier.height(dimens.lg))
            Row(
                Modifier
                    .fillMaxWidth()
                    .cascadeIn(cascade, "live")
                    .clip(Pulse.shapes.panel)
                    .background(colors.live.copy(alpha = 0.12f))
                    .border(dimens.hairline, colors.live.copy(alpha = 0.35f), Pulse.shapes.panel)
                    .padding(dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    LiveBadge(label = stringResource(Res.string.profile_live_now))
                    Spacer(Modifier.height(dimens.sm))
                    Text(live.title, style = Pulse.type.titleSmall, color = colors.textPrimary)
                }
                ViewerPill(live.viewers)
            }
        }
        Spacer(Modifier.height(dimens.xl))
        Text(stringResource(Res.string.profile_posts), style = Pulse.type.title, color = colors.textPrimary)
        Spacer(Modifier.height(dimens.md))
    }
}

private fun LazyListScope.creatorPosts(posts: ImmutableList<Post>, perRow: Int, cascade: CascadeState, onPost: (String) -> Unit) {
    items(posts.chunked(perRow), key = { row -> row.first().id }) { row ->
        Row(
            Modifier.padding(horizontal = Pulse.dimens.gutter, vertical = Pulse.dimens.sm),
            horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.md),
        ) {
            row.forEachIndexed { i, post ->
                PostTile(post, i, onOpen = { onPost(post.id) }, modifier = Modifier.weight(1f).cascadeIn(cascade, post.id))
            }
            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    item(key = "nav-space") {
        Column {
            Spacer(Modifier.height(Pulse.dimens.xxl))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun CreatorProfilePreview() = PreviewContainer {
    CreatorProfileContent(checkNotNull(CreatorProfileUiState.load(SampleData.creator.id)), {}, {})
}
