package com.airbnb.mvrx.workflow

import com.airbnb.mvrx.BaseMavericksViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Props
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface ViewState

fun doSomethingWith(output: Any) {
    TODO("Not yet implemented")
}

@InternalCoroutinesApi
fun main() {
    val calendarStore = Store<CalendarId, List<CalendarEvent>>()
    val loadingWorkflow = LoadingWorkflow(calendarStore)
    val calendarWorkflow = CalendarWorkflow(loadingWorkflow)

    //would be lifecycle scope most likely
    MainScope().launch {
        calendarWorkflow.startRootWorkflow("Loading Message") { output -> doSomethingWith(output) }
            .collect { }
    }
}



//Calendar workflow is used for displaying a list of calender events
//Rather than accessing services itself, this workflow will delegate loading to LoadingWorkflow
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class CalendarWorkflow(
    private val loadingWorkflow: LoadingWorkflow
) : BaseMavericksViewModel<CalendarWorkflow.State>(State.Loading, debugMode = false) {

    sealed class State : MvRxState {
        object Loading : State()
        internal data class DisplayingEvents(val calendarEvents: List<CalendarEvent>) : State()
    }

    sealed class ViewState : com.airbnb.mvrx.workflow.ViewState {
        data class Calendar(val calendarEvents: List<CalendarEvent>) : ViewState()
    }

    override suspend fun onStateChange(props: Props, state: State, output: (Any) -> Unit): com.airbnb.mvrx.workflow.ViewState {
        return when (state) {
            is State.DisplayingEvents -> ViewState.Calendar(state.calendarEvents)
            is State.Loading -> {
                val loadingViewState = loadingWorkflow.childViewState(props) { setState { State.DisplayingEvents(it as Events) } }
                return loadingViewState
            }
        }
    }
}

class LoadingWorkflow(private val eventStore: Store<CalendarId, List<CalendarEvent>>
) : BaseMavericksViewModel<LoadingWorkflow.LoadingState>(LoadingState, false) {
    object LoadingState : MvRxState

    sealed class LoadingViewState : com.airbnb.mvrx.workflow.ViewState {
        data class Loading(val msg: String = "Loading Calenders") : LoadingViewState()
    }

    override suspend fun onStateChange(props: Props, state: LoadingState, output: (Any) -> Unit): LoadingViewState {
        suspend { eventStore.get() }
            .execute {
                output(it()!!)
                this
            }
        return LoadingViewState.Loading() //sent using the parent workflow scope since we did not launch our own
    }
}



typealias CalendarId = String
typealias CalendarEvent = String
typealias Events = List<CalendarEvent>

class Store<T, V> {
    suspend fun get(): V = TODO()
}
