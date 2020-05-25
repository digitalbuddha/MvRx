package com.airbnb.mvrx.workflow

import com.airbnb.mvrx.Props
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest

@ExperimentalCoroutinesApi
abstract class Workflow<S> {
    val stateFlow: Flow<S> = flow {}
    val outputFlow = ConflatedBroadcastChannel<ViewState>()

    fun startRootWorkflow(props: Props): Flow<ViewState> {
        return stateFlow.mapLatest { state ->
            viewState(props, state)
        }
    }


    abstract fun viewState(props: Props, state: S): ViewState
}

@InternalCoroutinesApi
class CalWorkflow : Workflow<CalendarWorkflow.State>() {
    override fun viewState(props: Props, state: CalendarWorkflow.State): ViewState {
        return when (state) {
            is CalendarWorkflow.State.Loading -> LoadingWorkflow.LoadingViewState.Loading()
            else -> LoadingWorkflow.LoadingViewState.Loading()
        }
    }

}

@InternalCoroutinesApi
class LoadWorkflow : Workflow<LoadingWorkflow.LoadingState>() {
    override fun viewState(props: Props, state: LoadingWorkflow.LoadingState): ViewState {
        return LoadingWorkflow.LoadingViewState.Loading()
    }

}