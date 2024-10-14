package com.dmonster.smap.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowObserverInStop<T>(
    lifecycleOwner: LifecycleOwner,
    private val flow: Flow<T>,
    private val collector: suspend (T) -> Unit
) {
    private var job: Job? = null

    init {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    job = source.lifecycleScope.launch {
                        flow.collect {
                            collector(it)
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    job?.cancel()
                    job = null
                }
                else -> {}
            }
        })
    }
}

inline fun <reified T> Flow<T>.observeOnLifecycleStop(
    lifecycleOwner: LifecycleOwner, noinline collector: suspend (T) -> Unit
) = FlowObserverInStop(lifecycleOwner, this, collector)

// .onEach{ } 사용할때 사용
inline fun <reified T> Flow<T>.observeInLifecycleStop(
    lifecycleOwner: LifecycleOwner
) = FlowObserverInStop(lifecycleOwner, this) {}

class FlowObserverInDestroy<T>(
    lifecycleOwner: LifecycleOwner,
    private val flow: Flow<T>,
    private val collector: suspend (T) -> Unit
) {
    private var job: Job? = null

    init {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    job = source.lifecycleScope.launch {
                        flow.collect {
                            collector(it)
                        }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    job?.cancel()
                    job = null
                }
                else -> {}
            }
        })
    }
}

inline fun <reified T> Flow<T>.observeOnLifecycleDestroy(
    lifecycleOwner: LifecycleOwner, noinline collector: suspend (T) -> Unit
) = FlowObserverInDestroy(lifecycleOwner, this, collector)

inline fun <reified T> Flow<T>.observeInLifecycleDestroy(
    lifecycleOwner: LifecycleOwner
) = FlowObserverInDestroy(lifecycleOwner, this) {}