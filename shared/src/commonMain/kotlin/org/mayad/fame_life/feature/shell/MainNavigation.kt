package org.mayad.fame_life.feature.shell

import androidx.compose.runtime.Immutable

/**
 * Where the tabs can send the user. Built once by the nav host; the tabs only call these, so they never depend on how
 * navigation works.
 */
@Immutable
class MainNavigation(
    val openPost: (postId: String) -> Unit,
    val openCreator: (creatorId: String) -> Unit,
    val openLiveSetup: (title: String) -> Unit,
    val openSummary: (sessionId: String) -> Unit,
    val openSettings: () -> Unit,
    val openEditProfile: () -> Unit,
    val openPaywall: () -> Unit,
)
