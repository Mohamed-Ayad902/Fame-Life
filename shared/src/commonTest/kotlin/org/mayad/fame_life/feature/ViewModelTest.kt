package org.mayad.fame_life.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** ViewModels launch in Dispatchers.Main; tests swap it for an eager test dispatcher around each case. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    protected val dispatcher = UnconfinedTestDispatcher()

    @kotlin.test.BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(dispatcher)

    @kotlin.test.AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()
}
