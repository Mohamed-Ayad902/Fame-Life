package org.mayad.fame_life.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class BackStackNavigatorTest {
    private val stack = NavBackStack<NavKey>(AppRoute.Main)
    private val navigator = BackStackNavigator(stack)

    @Test
    fun pushIgnoresADoubleTapOnTheSameDestination() {
        navigator.push(AppRoute.PostDetail("p1"))
        navigator.push(AppRoute.PostDetail("p1"))
        navigator.push(AppRoute.PostDetail("p2"))
        assertEquals(listOf(AppRoute.Main, AppRoute.PostDetail("p1"), AppRoute.PostDetail("p2")), stack.toList())
    }

    @Test
    fun popNeverRemovesTheRoot() {
        navigator.pop()
        navigator.pop()
        assertEquals(listOf<NavKey>(AppRoute.Main), stack.toList())
    }

    @Test
    fun replaceTopSwapsTheFinishedLiveForItsSummary() {
        navigator.push(AppRoute.LiveSetup())
        navigator.replaceTop(AppRoute.Summary("s1"))
        assertEquals(listOf(AppRoute.Main, AppRoute.Summary("s1")), stack.toList())
    }

    @Test
    fun popToRootLeavesOnlyTheShell() {
        navigator.push(AppRoute.Settings)
        navigator.push(AppRoute.EditProfile)
        navigator.popToRoot()
        assertEquals(listOf<NavKey>(AppRoute.Main), stack.toList())
    }
}
