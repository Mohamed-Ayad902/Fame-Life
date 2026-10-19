package org.mayad.fame_life.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The navigation operations screens need. Screens never see this: the nav host turns it into plain callbacks, so a
 * screen only knows "open this post", not how the stack works.
 */
interface AppNavigator {
    fun push(route: AppRoute)
    fun pop()

    /** Replaces the top destination; used for live → summary so back never returns to a finished live. */
    fun replaceTop(route: AppRoute)

    /** Returns to the tab shell, dropping everything on top of it. */
    fun popToRoot()
}

/** [AppNavigator] over a Navigation 3 back stack. The root ([AppRoute.Main]) is never removed. */
class BackStackNavigator(private val backStack: NavBackStack<NavKey>) : AppNavigator {
    override fun push(route: AppRoute) {
        // Double taps must not stack the same screen twice.
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    override fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    override fun replaceTop(route: AppRoute) {
        if (backStack.size > 1) backStack[backStack.lastIndex] = route else backStack.add(route)
    }

    override fun popToRoot() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
