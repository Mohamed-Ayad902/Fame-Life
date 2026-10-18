package org.mayad.fame_life.feature.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.common_back
import famelife.shared.generated.resources.post_comments
import famelife.shared.generated.resources.post_comments_simulated
import famelife.shared.generated.resources.post_go_back
import famelife.shared.generated.resources.post_like
import famelife.shared.generated.resources.post_liked
import famelife.shared.generated.resources.post_more_from
import famelife.shared.generated.resources.post_not_liked
import famelife.shared.generated.resources.post_share
import famelife.shared.generated.resources.post_share_text
import famelife.shared.generated.resources.post_unavailable_body
import famelife.shared.generated.resources.post_unavailable_title
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.app.AppConfig
import org.mayad.fame_life.app.LocalAppContainer
import org.mayad.fame_life.app.Navigator
import org.mayad.fame_life.app.Route
import org.mayad.fame_life.core.analytics.AnalyticsEvent
import org.mayad.fame_life.core.designsystem.art.Artwork
import org.mayad.fame_life.core.designsystem.components.AtmosphereBackground
import org.mayad.fame_life.core.designsystem.components.CreatorAvatar
import org.mayad.fame_life.core.designsystem.components.EmptyState
import org.mayad.fame_life.core.designsystem.components.GlassIconButton
import org.mayad.fame_life.core.designsystem.components.SectionHeader
import org.mayad.fame_life.core.designsystem.components.SimulatedTag
import org.mayad.fame_life.core.designsystem.components.bleed
import org.mayad.fame_life.core.designsystem.components.pressable
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.core.designsystem.icons.PulseIcon
import org.mayad.fame_life.core.designsystem.motion.CascadeState
import org.mayad.fame_life.core.designsystem.motion.cascadeIn
import org.mayad.fame_life.core.designsystem.motion.rememberCascadeState
import org.mayad.fame_life.core.designsystem.preview.PreviewContainer
import org.mayad.fame_life.core.designsystem.preview.ScreenPreviews
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.platform.shareText
import org.mayad.fame_life.core.preview.SampleData
import org.mayad.fame_life.core.ui.timeAgoText
import org.mayad.fame_life.data.content.ArtMotif
import org.mayad.fame_life.data.content.ArtSpec
import org.mayad.fame_life.data.content.Creator
import org.mayad.fame_life.data.content.Post
import org.mayad.fame_life.data.content.PostComment
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.feature.home.PostTile

private const val ART_PARALLAX = 0.45f
private const val LIKE_POP = 1.18f
private const val MAX_ART_ASPECT = 1.35f
private const val ART_PALETTES = 8

@Immutable
data class PostDetailState(val post: Post, val creator: Creator, val comments: List<PostComment>, val more: List<Post>)

internal fun postDetailState(postId: String): PostDetailState? {
    val post = SeedContent.post(postId) ?: return null
    val creator = SeedContent.creator(post.creatorId) ?: return null
    return PostDetailState(post, creator, SeedContent.commentsFor(post), SeedContent.postsBy(creator.id).filter { it.id != postId })
}

/** Tier 1: loads the post, logs the view, owns the in-session like and turns taps into navigation. */
@Composable
fun PostDetailScreen(postId: String, navigator: Navigator) {
    val container = LocalAppContainer.current
    val state = remember(postId) { postDetailState(postId) }
    if (state == null) {
        AtmosphereBackground {
            EmptyState(
                Glyphs.Info,
                Gloss.Slate,
                stringResource(Res.string.post_unavailable_title),
                stringResource(Res.string.post_unavailable_body),
                Modifier.align(Alignment.Center),
                stringResource(Res.string.post_go_back),
                { navigator.pop() },
            )
        }
        return
    }
    LaunchedEffect(postId) { container.analytics.log(AnalyticsEvent.PostViewed(state.post.category.name.lowercase())) }
    // Likes are a local, in-session interaction with simulated content; nothing is sent anywhere.
    var liked by rememberSaveable(postId) { mutableStateOf(false) }
    val shareMessage = stringResource(Res.string.post_share_text, state.post.caption, state.creator.handle, AppConfig.APP_NAME)
    PostDetailContent(
        state = state,
        liked = liked,
        onLike = { liked = !liked },
        onShare = { shareText(shareMessage) },
        onBack = { navigator.pop() },
        onCreator = { navigator.push(Route.CreatorProfile(it)) },
        onPost = { navigator.push(Route.PostDetail(it)) },
    )
}

/** Tier 2: stateless. Phones scroll art then text; wide windows pin the art on the start pane. */
@Composable
fun PostDetailContent(
    state: PostDetailState,
    liked: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
) {
    val list = rememberLazyListState()
    val cascade = rememberCascadeState()
    Box(Modifier.fillMaxSize().background(Pulse.colors.background)) {
        if (Pulse.width.split) {
            Row(Modifier.fillMaxSize()) {
                PostArt(state.post, Modifier.weight(1f).fillMaxHeight(), parallax = { 0f })
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBars),
                    contentPadding = PaddingValues(top = Pulse.dimens.xl, bottom = Pulse.dimens.xxxl),
                ) { postBody(state, liked, cascade, onLike, onShare, onCreator, onPost) }
            }
        } else {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Pulse.dimens.xxxl)) {
                item(key = "art") {
                    PostArt(
                        state.post,
                        Modifier.fillMaxWidth().aspectRatio(1f / state.post.aspect.coerceIn(1f, MAX_ART_ASPECT)),
                        parallax = { if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset * ART_PARALLAX else 0f },
                    )
                }
                postBody(state, liked, cascade, onLike, onShare, onCreator, onPost)
            }
        }
        GlassIconButton(
            Glyphs.ChevronLeft,
            contentDescription = stringResource(Res.string.common_back),
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(Pulse.dimens.lg),
        )
    }
}

@Composable
private fun PostArt(post: Post, modifier: Modifier, parallax: () -> Float) {
    val background = Pulse.colors.background
    Box(modifier) {
        // Parallax is read inside the layer, so scrolling never recomposes the art.
        Artwork(post.art, Modifier.fillMaxSize().graphicsLayer { translationY = parallax() })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to background)))
    }
}

/** Tier 3: the text column, shared by both layouts. */
private fun LazyListScope.postBody(
    state: PostDetailState,
    liked: Boolean,
    cascade: CascadeState,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onCreator: (String) -> Unit,
    onPost: (String) -> Unit,
) {
    item(key = "body") { PostHeader(state, liked, cascade, onLike, onShare, onCreator) }
    items(state.comments, key = { it.id }) { comment -> CommentRow(comment, Modifier.cascadeIn(cascade, comment.id)) }
    if (state.more.isNotEmpty()) {
        item(key = "more") {
            val dimens = Pulse.dimens
            Column(Modifier.padding(start = dimens.gutter, end = dimens.gutter, top = dimens.xxl).cascadeIn(cascade, "more")) {
                SectionHeader(stringResource(Res.string.post_more_from, state.creator.displayName.substringBefore(' ')))
                Spacer(Modifier.height(dimens.md))
                LazyRow(Modifier.bleed(dimens.gutter), contentPadding = PaddingValues(horizontal = dimens.gutter), horizontalArrangement = Arrangement.spacedBy(dimens.md)) {
                    items(state.more, key = { it.id }) { other ->
                        PostTile(other, 0, onOpen = { onPost(other.id) }, modifier = Modifier.width(dimens.railCardWide))
                    }
                }
            }
        }
    }
    item(key = "nav-space") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
}

@Composable
private fun PostHeader(
    state: PostDetailState,
    liked: Boolean,
    cascade: CascadeState,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onCreator: (String) -> Unit,
) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val post = state.post
    val creator = state.creator
    val heartScale = animateFloatAsState(if (liked) LIKE_POP else 1f, Motion.press(), label = "like")
    val likedState = stringResource(if (liked) Res.string.post_liked else Res.string.post_not_liked)
    Column(Modifier.padding(horizontal = dimens.gutter)) {
        Row(
            Modifier.fillMaxWidth().cascadeIn(cascade, "creator").pressable(pressedScale = 0.98f) { onCreator(creator.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreatorAvatar(creator.avatar, dimens.avatarMd)
            Spacer(Modifier.width(dimens.md))
            Column(Modifier.weight(1f)) {
                Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.textPrimary)
                Text("@${creator.handle} · ${timeAgoText(post.hoursAgo)}", style = Pulse.type.meta, color = colors.textMuted)
            }
            SimulatedTag()
        }
        Spacer(Modifier.height(dimens.lg))
        Text(post.caption, style = Pulse.type.headline, color = colors.textPrimary, modifier = Modifier.cascadeIn(cascade, "caption"))
        Spacer(Modifier.height(dimens.sm))
        Text(post.tags.joinToString("  ") { "#$it" }, style = Pulse.type.label, color = colors.cyan)
        Spacer(Modifier.height(dimens.xl))
        Row(Modifier.cascadeIn(cascade, "actions"), horizontalArrangement = Arrangement.spacedBy(dimens.sm)) {
            Row(
                Modifier
                    .clip(Pulse.shapes.pill)
                    .background(if (liked) colors.live.copy(alpha = 0.16f) else colors.surface)
                    .pressable(role = Role.Switch, onClick = onLike)
                    .semantics { stateDescription = likedState }
                    .padding(horizontal = dimens.lg, vertical = dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseIcon(
                    if (liked) Glyphs.HeartFilled else Glyphs.Heart,
                    if (liked) colors.live else colors.textPrimary,
                    Modifier.graphicsLayer { val s = heartScale.value; scaleX = s; scaleY = s },
                    size = dimens.icon,
                    contentDescription = stringResource(Res.string.post_like),
                )
                Spacer(Modifier.width(dimens.sm))
                Text(compactCount(post.likes + if (liked) 1 else 0), style = Pulse.type.label, color = colors.textPrimary)
            }
            StatPill(Glyphs.Comment, compactCount(post.comments))
            StatPill(Glyphs.Share, compactCount(post.shares), stringResource(Res.string.post_share), onShare)
        }
        Spacer(Modifier.height(dimens.xxl))
        SectionHeader(
            stringResource(Res.string.post_comments),
            eyebrow = stringResource(Res.string.post_comments_simulated, compactCount(post.comments)),
        )
        Spacer(Modifier.height(dimens.sm))
    }
}

@Composable
private fun CommentRow(comment: PostComment, modifier: Modifier = Modifier) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    val hash = SeedContent.stableHash(comment.author)
    Row(modifier.padding(horizontal = dimens.gutter, vertical = dimens.sm)) {
        CreatorAvatar(ArtSpec(hash.mod(ART_PALETTES), ArtMotif.PORTRAIT, hash), dimens.avatarSm)
        Spacer(Modifier.width(dimens.md))
        Column(Modifier.weight(1f)) {
            Text("${comment.author} · ${timeAgoText(comment.hoursAgo)}", style = Pulse.type.meta, color = colors.textMuted)
            Text(comment.text, style = Pulse.type.body, color = colors.textPrimary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PulseIcon(Glyphs.Heart, colors.textMuted, size = dimens.iconSmall)
            Text(compactCount(comment.likes), style = Pulse.type.meta, color = colors.textMuted)
        }
    }
}

@Composable
private fun StatPill(glyph: ImageVector, value: String, description: String? = null, onClick: (() -> Unit)? = null) {
    val colors = Pulse.colors
    val dimens = Pulse.dimens
    Row(
        Modifier
            .clip(Pulse.shapes.pill)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = dimens.lg, vertical = dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(glyph, colors.textPrimary, size = dimens.icon, contentDescription = description)
        Spacer(Modifier.width(dimens.sm))
        Text(value, style = Pulse.type.label, color = colors.textPrimary)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Previews

@ScreenPreviews
@Composable
private fun PostDetailPreview() = PreviewContainer {
    PostDetailContent(postDetailState(SampleData.post.id)!!, liked = true, {}, {}, {}, {}, {})
}
