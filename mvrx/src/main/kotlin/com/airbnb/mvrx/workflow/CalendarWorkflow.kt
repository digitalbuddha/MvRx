package com.airbnb.mvrx.workflow

import com.airbnb.mvrx.BaseMavericksViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Props
import com.airbnb.mvrx.workflow.LoadingWorkflow.CalendarEvent
import com.airbnb.mvrx.workflow.LoadingWorkflow.CalendarId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ViewState

fun doSomethingWith(output: Any) {
    TODO("Not yet implemented")
}

@InternalCoroutinesApi
fun main() {
    //would be lifecycle scope most likely
    val mainScope = MainScope()
    val calendarStore = Store<CalendarId, List<CalendarEvent>>()
    val loadingWorkflow = LoadingWorkflow(calendarStore)
    val calendarWorkflow = CalendarWorkflow(loadingWorkflow)


    mainScope.launch {
        calendarWorkflow.startRootWorkflow("Loading Message") { output -> doSomethingWith(output) }
            .collect { }
    }
}


//Calendar workflow is used for displaying a list of calender events
//Rather than accessing services itself, this workflow will delegate loading to LoadingWorkflow
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class CalendarWorkflow(
    private val loadingWorkflow: LoadingWorkflow) : BaseMavericksViewModel<CalendarWorkflow.State>(State.Loading, debugMode = false) {

    sealed class State : MvRxState {
        object Loading : State()
        internal data class DisplayingEvents(val calendarEvents: List<CalendarEvent>) : State()
    }

    sealed class ViewState : com.airbnb.mvrx.workflow.ViewState {
        data class Calendar(val calendarEvents: List<CalendarEvent>) : ViewState()
    }


    override suspend fun FlowCollector<com.airbnb.mvrx.workflow.ViewState>.onStateChange(state: State, props: Props, output: (Any) -> Unit) {
        when (state) {
            is State.DisplayingEvents -> emit(ViewState.Calendar(state.calendarEvents))
            is State.Loading -> {
                emitAll(loadingWorkflow.childViewState(props) { setState { State.DisplayingEvents(it as Events) } })
            }
        }
    }
}


class LoadingWorkflow(private val eventStore: Store<CalendarId, List<CalendarEvent>>
) : BaseMavericksViewModel<LoadingWorkflow.LoadingState>(LoadingState, false) {
    object LoadingState : MvRxState

    sealed class LoadingViewState : ViewState {
        data class Loading(val msg: String = "Loading Calenders") : LoadingViewState()
    }

    override suspend fun FlowCollector<ViewState>.onStateChange(state: LoadingState, props: Props, output: (Any) -> Unit) {
        emit(LoadingViewState.Loading())//sent using the parent workflow scope since we did not launch our own
        withContext(viewModelScope.coroutineContext) { output(eventStore.get()) }
    }
}




typealias CalendarId = String
typealias CalendarEvent = String
typealias Events = List<CalendarEvent>

class Store<T, V> {
    suspend fun get(): V = TODO()
}