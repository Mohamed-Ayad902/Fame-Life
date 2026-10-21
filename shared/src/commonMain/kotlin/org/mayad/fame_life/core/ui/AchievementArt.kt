package org.mayad.fame_life.core.ui

import androidx.compose.ui.graphics.vector.ImageVector
import famelife.shared.generated.resources.Res
import famelife.shared.generated.resources.badge_crowd
import famelife.shared.generated.resources.badge_crowd_detail
import famelife.shared.generated.resources.badge_first_live
import famelife.shared.generated.resources.badge_first_live_detail
import famelife.shared.generated.resources.badge_gifted
import famelife.shared.generated.resources.badge_gifted_detail
import famelife.shared.generated.resources.badge_regular
import famelife.shared.generated.resources.badge_regular_detail
import famelife.shared.generated.resources.badge_rising
import famelife.shared.generated.resources.badge_rising_detail
import famelife.shared.generated.resources.badge_storyteller
import famelife.shared.generated.resources.badge_storyteller_detail
import org.jetbrains.compose.resources.StringResource
import org.mayad.fame_life.core.designsystem.icons.Gloss
import org.mayad.fame_life.core.designsystem.icons.Glyphs
import org.mayad.fame_life.domain.progression.Achievement

/** Copy and art for each [Achievement]; the domain only names them. */
val Achievement.titleRes: StringResource
    get() = when (this) {
        Achievement.FIRST_LIVE -> Res.string.badge_first_live
        Achievement.CROWD -> Res.string.badge_crowd
        Achievement.GIFTED -> Res.string.badge_gifted
        Achievement.STORYTELLER -> Res.string.badge_storyteller
        Achievement.REGULAR -> Res.string.badge_regular
        Achievement.RISING -> Res.string.badge_rising
    }

val Achievement.detailRes: StringResource
    get() = when (this) {
        Achievement.FIRST_LIVE -> Res.string.badge_first_live_detail
        Achievement.CROWD -> Res.string.badge_crowd_detail
        Achievement.GIFTED -> Res.string.badge_gifted_detail
        Achievement.STORYTELLER -> Res.string.badge_storyteller_detail
        Achievement.REGULAR -> Res.string.badge_regular_detail
        Achievement.RISING -> Res.string.badge_rising_detail
    }

val Achievement.glyph: ImageVector
    get() = when (this) {
        Achievement.FIRST_LIVE -> Glyphs.Mic
        Achievement.CROWD -> Glyphs.Users
        Achievement.GIFTED -> Glyphs.Gift
        Achievement.STORYTELLER -> Glyphs.Book
        Achievement.REGULAR -> Glyphs.Bolt
        Achievement.RISING -> Glyphs.Star
    }

val Achievement.gloss: Gloss
    get() = when (this) {
        Achievement.FIRST_LIVE -> Gloss.Violet
        Achievement.CROWD -> Gloss.Blue
        Achievement.GIFTED -> Gloss.Gold
        Achievement.STORYTELLER -> Gloss.Cyan
        Achievement.REGULAR -> Gloss.Mint
        Achievement.RISING -> Gloss.Coral
    }
