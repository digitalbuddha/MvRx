package com.airbnb.mvrx.workflow

import com.airbnb.mvrx.BaseMavericksViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Props
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface Screen


@InternalCoroutinesApi
fun main() {
    val calendarStore = Store<CalendarId, List<CalendarEvent>>()
    val loadingWorkflow = LoadingWorkflow(calendarStore)
    val calendarWorkflow = CalendarWorkflow(loadingWorkflow)

    //would be lifecycle scope most likely
    MainScope().launch {
        calendarWorkflow.startRendering("Loading Message") { output -> doSomethingWith(output) }
        .collect {
            renderScreens(it)
        }
    }
}

fun doSomethingWith(output: Any) {
    TODO("Not yet implemented")
}

fun renderScreens(it: Screen) {
    TODO("Not yet implemented")
}

//Calendar workflow is used for displaying a list of calender events
//Rather than accessing services itself, this workflow will delegate loading to LoadingWorkflow
@InternalCoroutinesApi
class CalendarWorkflow(
    private val loadingWorkflow: LoadingWorkflow
) : BaseMavericksViewModel<CalendarWorkflow.State>(State.Loading, debugMode = false) {

    sealed class State : MvRxState {
        object Loading : State()
        internal data class DisplayingEvents(val calendarEvents: List<CalendarEvent>) : State()
    }

    sealed class Rendering : Screen {
        data class Calendar(val calendarEvents: List<CalendarEvent>) : Rendering()
    }

    override suspend fun render(state: State, props: Props, output: (Any) -> Unit) {
        when (state) {
            is State.Loading -> {
                //we could also collect and emit something different here
                loadingWorkflow.startRendering(props) { setState { State.DisplayingEvents(it as Events) } }
                    .collect { renderings.send(it) }
            }
            is State.DisplayingEvents -> renderings.send(Rendering.Calendar(state.calendarEvents))
        }
    }
}

//Has 1 state: Loading - When a parent workflow renders our initial state we will Rendering a loading data model and fetch data
//on data result we will set an output on a parent workflow which should trigger displaying of items
class LoadingWorkflow(private val eventStore: Store<CalendarId, List<CalendarEvent>>
) : BaseMavericksViewModel<LoadingWorkflow.LoadingState>(LoadingState, false) {
    object LoadingState : MvRxState

    sealed class Rendering : Screen {
        data class Loading(val msg: String = "Loading Calenders") : Rendering()
    }

    override suspend fun render(state: LoadingState, props: Props, output: (Any) -> Unit) {
        renderings.send(Rendering.Loading()) //sent using the parent workflow scope since we did not launch our own
        viewModelScope
            .launch {
                output.invoke(eventStore.get())
            }
            .invokeOnCompletion { onCleared() } //alternatively we can wait for our parent scope to clean us up
    }
}


typealias CalendarId = String
typealias CalendarEvent = String
typealias Events = List<CalendarEvent>

class Store<T, V> {
    fun get(): V = TODO()
}
