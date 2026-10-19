package org.mayad.fame_life

import androidx.compose.ui.window.ComposeUIViewController
import org.mayad.fame_life.di.initKoin

fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { App() }
