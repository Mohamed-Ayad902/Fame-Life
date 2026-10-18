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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import org.mayad.fame_life.core.designsystem.theme.Motion
import org.mayad.fame_life.core.designsystem.theme.Pulse
import org.mayad.fame_life.core.designsystem.theme.Shapes
import org.mayad.fame_life.core.designsystem.theme.Space
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.core.format.hoursAgoLabel
import org.mayad.fame_life.core.platform.shareText
import org.mayad.fame_life.data.content.SeedContent
import org.mayad.fame_life.feature.home.PostTile

@Composable
fun PostDetailScreen(postId: String, navigator: Navigator) {
    val post = remember(postId) { SeedContent.post(postId) }
    val creator = post?.let { SeedContent.creator(it.creatorId) }
    val container = LocalAppContainer.current
    if (post == null || creator == null) {
        AtmosphereBackground {
            EmptyState(Glyphs.Info, Gloss.Slate, "This post isn't available", "It may have been removed from the feed.", Modifier.align(Alignment.Center), "Go back", { navigator.pop() })
        }
        return
    }
    LaunchedEffect(postId) { container.analytics.log(AnalyticsEvent.PostViewed(post.category.name.lowercase())) }
    val comments = remember(postId) { SeedContent.commentsFor(post) }
    val more = remember(postId) { SeedContent.postsBy(creator.id).filter { it.id != postId } }
    // Likes are a local, in-session interaction with simulated content; nothing is sent anywhere.
    var liked by rememberSaveable(postId) { mutableStateOf(false) }
    val heartScale by animateFloatAsState(if (liked) 1.18f else 1f, Motion.press(), label = "like")
    val list = rememberLazyListState()
    val colors = Pulse.colors

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.xxxl)) {
            item(key = "art") {
                Box(Modifier.fillMaxWidth().aspectRatio(1f / post.aspect.coerceIn(1f, 1.35f))) {
                    Artwork(
                        post.art,
                        Modifier.fillMaxSize().graphicsLayer {
                            val offset = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset.toFloat() else 0f
                            translationY = offset * 0.45f
                        },
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to colors.background)))
                }
            }
            item(key = "body") {
                Column(Modifier.padding(horizontal = Space.screen)) {
                    Row(
                        Modifier.fillMaxWidth().pressable(pressedScale = 0.98f) { navigator.push(Route.CreatorProfile(creator.id)) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CreatorAvatar(creator.avatar, 44.dp)
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(creator.displayName, style = Pulse.type.titleSmall, color = colors.textPrimary)
                            Text("@${creator.handle} · ${hoursAgoLabel(post.hoursAgo)}", style = Pulse.type.meta, color = colors.textMuted)
                        }
                        SimulatedTag()
                    }
                    Spacer(Modifier.height(Space.lg))
                    Text(post.caption, style = Pulse.type.headline, color = colors.textPrimary)
                    Spacer(Modifier.height(Space.sm))
                    Text(post.tags.joinToString("  ") { "#$it" }, style = Pulse.type.label, color = colors.cyan)
                    Spacer(Modifier.height(Space.xl))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Row(
                            Modifier
                                .clip(Shapes.pill)
                                .background(if (liked) colors.live.copy(alpha = 0.16f) else colors.surface)
                                .pressable(role = Role.Switch) { liked = !liked }
                                .semantics { stateDescription = if (liked) "Liked" else "Not liked" }
                                .padding(horizontal = Space.lg, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulseIcon(
                                if (liked) Glyphs.HeartFilled else Glyphs.Heart,
                                if (liked) colors.live else colors.textPrimary,
                                Modifier.graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                                size = 20.dp,
                                contentDescription = "Like",
                            )
                            Spacer(Modifier.width(Space.sm))
                            Text(compactCount(post.likes + if (liked) 1 else 0), style = Pulse.type.label, color = colors.textPrimary)
                        }
                        StatPill(Glyphs.Comment, compactCount(post.comments))
                        Row(
                            Modifier
                                .clip(Shapes.pill)
                                .background(colors.surface)
                                .pressable {
                                    shareText("\"${post.caption}\" by @${creator.handle} — a simulated post in Pulse")
                                }
                                .padding(horizontal = Space.lg, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulseIcon(Glyphs.Share, colors.textPrimary, size = 20.dp, contentDescription = "Share")
                            Spacer(Modifier.width(Space.sm))
                            Text(compactCount(post.shares), style = Pulse.type.label, color = colors.textPrimary)
                        }
                    }
                    Spacer(Modifier.height(Space.xxl))
                    SectionHeader("Comments", eyebrow = "${compactCount(post.comments)} simulated")
                    Spacer(Modifier.height(Space.sm))
                }
            }
            items(comments, key = { it.id }) { comment ->
                Row(Modifier.padding(horizontal = Space.screen, vertical = Space.sm)) {
                    CreatorAvatar(org.mayad.fame_life.data.content.ArtSpec(SeedContent.stableHash(comment.author).mod(8), org.mayad.fame_life.data.content.ArtMotif.PORTRAIT, SeedContent.stableHash(comment.author)), 34.dp)
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text("${comment.author} · ${hoursAgoLabel(comment.hoursAgo)}", style = Pulse.type.meta, color = colors.textMuted)
                        Text(comment.text, style = Pulse.type.body, color = colors.textPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PulseIcon(Glyphs.Heart, colors.textMuted, size = 16.dp)
                        Text(compactCount(comment.likes), style = Pulse.type.meta, color = colors.textMuted)
                    }
                }
            }
            if (more.isNotEmpty()) {
                item(key = "more") {
                    Column(Modifier.padding(start = Space.screen, end = Space.screen, top = Space.xxl)) {
                        SectionHeader("More from ${creator.displayName.substringBefore(' ')}")
                        Spacer(Modifier.height(Space.md))
                        LazyRow(Modifier.bleed(Space.screen), contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                            items(more, key = { it.id }) { other ->
                                PostTile(other, 0, onOpen = { navigator.push(Route.PostDetail(other.id)) }, modifier = Modifier.width(160.dp))
                            }
                        }
                    }
                }
            }
            item(key = "nav-space") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
        GlassIconButton(
            Glyphs.ChevronLeft,
            contentDescription = "Back",
            onClick = { navigator.pop() },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(Space.lg),
        )
    }
}

@Composable
private fun StatPill(glyph: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    val colors = Pulse.colors
    Row(
        Modifier
            .clip(Shapes.pill)
            .background(colors.surface)
            .padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseIcon(glyph, colors.textPrimary, size = 20.dp)
        Spacer(Modifier.width(Space.sm))
        Text(value, style = Pulse.type.label, color = colors.textPrimary)
    }
}
