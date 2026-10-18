package org.mayad.fame_life.core.ui

import androidx.compose.runtime.Composable
import famelife.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mayad.fame_life.core.format.compactCount
import org.mayad.fame_life.data.content.Category
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.profile.ProfileFieldError
import org.mayad.fame_life.domain.profile.ProfileRules
import org.mayad.fame_life.domain.progression.FameLevel
import org.mayad.fame_life.domain.simulation.AudienceLanguage
import org.mayad.fame_life.domain.simulation.AudiencePreset
import org.mayad.fame_life.domain.simulation.AudienceVibe
import org.mayad.fame_life.domain.simulation.EventFrequency

/**
 * The bridge from domain values to localized copy. Domain code stays free of display strings; screens call these.
 */
val FameLevel.titleRes: StringResource
    get() = when (this) {
        FameLevel.NEW_CREATOR -> Res.string.level_new_creator
        FameLevel.RISING_CREATOR -> Res.string.level_rising_creator
        FameLevel.KNOWN_CREATOR -> Res.string.level_known_creator
        FameLevel.INFLUENCER -> Res.string.level_influencer
        FameLevel.CELEBRITY -> Res.string.level_celebrity
        FameLevel.SUPERSTAR -> Res.string.level_superstar
    }

@Composable
fun FameLevel.title(): String = stringResource(titleRes)

@Composable
fun Category.label(): String = stringResource(
    when (this) {
        Category.GAMING -> Res.string.category_gaming
        Category.MUSIC -> Res.string.category_music
        Category.CHATTING -> Res.string.category_chatting
        Category.STORYTELLING -> Res.string.category_storytelling
        Category.LIFESTYLE -> Res.string.category_lifestyle
        Category.COMEDY -> Res.string.category_comedy
        Category.ENTERTAINMENT -> Res.string.category_entertainment
    },
)

@Composable
fun AudiencePreset.label(): String = stringResource(
    when (this) {
        AudiencePreset.INTIMATE -> Res.string.preset_intimate
        AudiencePreset.RISING -> Res.string.preset_rising
        AudiencePreset.BUZZING -> Res.string.preset_buzzing
        AudiencePreset.VIRAL -> Res.string.preset_viral
    },
)

@Composable
fun AudiencePreset.rangeLabel(): String =
    stringResource(Res.string.preset_range, compactCount(startRange.first), compactCount(targetRange.first))

@Composable
fun AudienceVibe.label(): String = stringResource(
    when (this) {
        AudienceVibe.SUPPORTIVE -> Res.string.vibe_supportive
        AudienceVibe.CURIOUS -> Res.string.vibe_curious
        AudienceVibe.FUNNY -> Res.string.vibe_funny
        AudienceVibe.HYPE -> Res.string.vibe_hype
        AudienceVibe.CRITICAL -> Res.string.vibe_critical
    },
)

@Composable
fun EventFrequency.label(): String = stringResource(
    when (this) {
        EventFrequency.CALM -> Res.string.frequency_calm
        EventFrequency.LIVELY -> Res.string.frequency_lively
        EventFrequency.WILD -> Res.string.frequency_wild
    },
)

@Composable
fun AudienceLanguage.label(): String = stringResource(
    when (this) {
        AudienceLanguage.ENGLISH -> Res.string.language_english
        AudienceLanguage.ARABIC -> Res.string.language_arabic
        AudienceLanguage.MIXED -> Res.string.language_mixed
    },
)

/** "45 s", "12 min", "1 h 05 min". */
@Composable
fun durationText(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return when {
        s < SECONDS_PER_MINUTE -> stringResource(Res.string.duration_seconds, s)
        s < SECONDS_PER_HOUR -> stringResource(Res.string.duration_minutes, s / SECONDS_PER_MINUTE)
        else -> stringResource(
            Res.string.duration_hours,
            s / SECONDS_PER_HOUR,
            ((s % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toString().padStart(2, '0'),
        )
    }
}

/** "now", "5h", "3d". */
@Composable
fun timeAgoText(hours: Int): String = when {
    hours <= 0 -> stringResource(Res.string.time_now)
    hours < HOURS_PER_DAY -> stringResource(Res.string.time_hours, hours)
    else -> stringResource(Res.string.time_days, hours / HOURS_PER_DAY)
}

/** Null for failures the user caused on purpose (closing the picker), which need no message. */
val AuthErrorKind.messageRes: StringResource?
    get() = when (this) {
        AuthErrorKind.CANCELLED -> null
        AuthErrorKind.NO_GOOGLE_ACCOUNT -> Res.string.auth_error_no_account
        AuthErrorKind.WRONG_ACCOUNT -> Res.string.auth_error_wrong_account
        AuthErrorKind.TOO_MANY_REQUESTS -> Res.string.auth_error_too_many
        AuthErrorKind.NETWORK -> Res.string.auth_error_network
        AuthErrorKind.REQUIRES_RECENT_LOGIN -> Res.string.auth_error_recent_login
        AuthErrorKind.NOT_CONFIGURED -> Res.string.auth_error_not_configured
        AuthErrorKind.UNKNOWN -> Res.string.auth_error_unknown
    }

@Composable
fun ProfileFieldError.message(maxLength: Int): String = when (this) {
    ProfileFieldError.EMPTY -> stringResource(Res.string.profile_error_name_empty)
    ProfileFieldError.TOO_SHORT -> stringResource(Res.string.profile_error_too_short, ProfileRules.USERNAME_MIN)
    ProfileFieldError.TOO_LONG -> stringResource(Res.string.profile_error_too_long, maxLength)
    ProfileFieldError.INVALID_CHARACTERS -> stringResource(Res.string.profile_error_characters)
    ProfileFieldError.BAD_DOTS -> stringResource(Res.string.profile_error_dots)
    ProfileFieldError.RESERVED -> stringResource(Res.string.profile_error_reserved)
}

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
private const val HOURS_PER_DAY = 24
