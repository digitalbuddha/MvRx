package com.airbnb.mvrx.workflow

import com.airbnb.mvrx.BaseMavericksViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Props
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

interface Screen


@InternalCoroutinesApi
fun main() {
    val calendarStore = Store<CalendarId, List<CalendarEvent>>()
    val loadingWorkflow = LoadingWorkflow(calendarStore)
    val calendarWorkflow = CalendarWorkflow(loadingWorkflow)

    //would be lifecycle scope most likely
    MainScope().launch {
        //render takes a lambda which is called when a child workflow invokes output
        //render returns a flow which will get all renderings emissions
        calendarWorkflow.render("Loading Message") { output -> doSomethingWith(output) }
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

    //TODO: when we stop rendering a child, how do we clear it?
    override suspend fun render(props: Props, output: (Any) -> Unit): Flow<Screen> {
        return flow {
            stateFlow.collect {
                when (it) {
                    is State.Loading -> {
                        //we could also collect and emit something different here
                        emitAll(loadingWorkflow.render(props, outputAction = { setState { State.DisplayingEvents(it as Events) } })) //TODO fix paramterized types in base class
                    }
                    is State.DisplayingEvents -> emit(Rendering.Calendar(it.calendarEvents))
                }
            }
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

    override suspend fun render(props: Props, outputAction: (Any) -> Unit): Flow<Screen> =
        flow {
            emit(Rendering.Loading())
            viewModelScope.launch {
                async { eventStore.get() }.execute {
                    outputAction.invoke(it.invoke()!!)  //setting output should trigger the parent to stop rendering this workflow
                    this
                }
            }
        }
}


typealias CalendarId = String
typealias CalendarEvent = String
typealias Events = List<CalendarEvent>

class Store<T, V> {
    suspend fun get(): V = TODO()
}
