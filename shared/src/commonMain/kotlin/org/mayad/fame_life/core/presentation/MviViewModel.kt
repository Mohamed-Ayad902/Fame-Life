package org.mayad.fame_life.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The shape every screen ViewModel follows:
 * - one immutable [state] the screen renders;
 * - [onIntent] for everything the user does;
 * - [events] for one-time results the screen must act on (navigate after saving, show a share sheet), delivered once.
 *
 * Pure UI navigation (open a post) does not go through the ViewModel; the screen calls its navigation callback.
 */
abstract class MviViewModel<S, I, E>(initial: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val eventChannel = Channel<E>(Channel.BUFFERED)
    val events: Flow<E> = eventChannel.receiveAsFlow()

    protected val currentState: S get() = mutableState.value

    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) = mutableState.update(reduce)

    protected fun emit(event: E) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

/** Marker for ViewModels that have no one-time events. */
object NoEvent

/** Collects [events] while the screen is shown and hands each one to [onEvent] exactly once. */
@Composable
fun <E> CollectEvents(events: Flow<E>, onEvent: (E) -> Unit) {
    val handler = rememberUpdatedState(onEvent)
    LaunchedEffect(events) { events.collect { handler.value(it) } }
}
