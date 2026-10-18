package org.mayad.fame_life.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.mayad.fame_life.core.designsystem.theme.Pulse

/**
 * Two panes side by side for Medium/Expanded windows (landscape phones, tablets, unfolded foldables). The leading
 * pane carries the visual, the trailing pane the reading and actions, capped at a comfortable line length.
 * Screens choose this or their portrait layout with `Pulse.width.split`.
 */
@Composable
fun SplitPanes(
    modifier: Modifier = Modifier,
    leadingWeight: Float = 1f,
    trailingWeight: Float = 1f,
    leading: @Composable BoxScope.() -> Unit,
    trailing: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Pulse.dimens.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(leadingWeight).fillMaxHeight(), contentAlignment = Alignment.Center, content = leading)
        Box(Modifier.weight(trailingWeight).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.widthIn(max = Pulse.dimens.contentMaxWidth).fillMaxWidth(), content = trailing)
        }
    }
}
