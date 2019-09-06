package com.klitsie.scopedobserver

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

interface Observer<T> : CoroutineScope {
	var onChange: Observer<T>.(T) -> Unit
	fun notify(value: T)
}

class Observable<T> constructor(defaultValue: T, lifecycleScope: CoroutineScope? = null) :
	CoroutineScope {

	private var observers = mapOf<Observer<T>, CancellableContinuation<T>>()

	private val job = SupervisorJob(lifecycleScope?.coroutineContext?.get(Job))

	override val coroutineContext = job + Dispatchers.Unconfined

	var value: T = defaultValue
		set(value) {
			field = value
			notifyObservers()
		}

	fun observe(
		lifeCycleAwareComponent: LifecycleOwner,
		onChange: Observer<T>.(T) -> Unit): Observer<T> =
		addObserver(LifeObserver(lifeCycleAwareComponent, Job(job), onChange))

	fun observe(
		scope: CoroutineScope,
		onChange: Observer<T>.(T) -> Unit): Observer<T> = observe(scope.coroutineContext, onChange)

	fun observe(
		context: CoroutineContext = Dispatchers.Unconfined,
		onChange: Observer<T>.(T) -> Unit): Observer<T> =
		addObserver(ScopedObserver(context + Job(job), onChange))

	private fun <R : Observer<T>> addObserver(observer: R) = observer.also {
		observer.launch {
			observer.notify(value)
			suspendObserver(observer)
		}
	}

	private suspend fun suspendObserver(observer: Observer<T>) {
		observer.notify(suspendCancellableCoroutine { cont ->
			cont.invokeOnCancellation {
				observers = observers.filterKeys { it != observer }
			}
			observers = observers + (observer to cont)
		})
	}

	private fun notifyObservers() {
		observers.let { cachedObservers ->
			observers = mapOf()
			cachedObservers.forEach { (observer, continuation) ->
				observer.launch {
					if (observer.isActive && continuation.isActive) {
						continuation.resume(value)
						suspendObserver(observer)
					}
				}
			}
		}
	}
}

internal class ScopedObserver<T>(
	override val coroutineContext: CoroutineContext,
	override var onChange: Observer<T>.(T) -> Unit) : Observer<T> {

	override fun notify(value: T) {
		onChange(value)
	}
}

internal class LifeObserver<T>(
	private val lifecycleOwner: LifecycleOwner,
	parentContext: CoroutineContext,
	override var onChange: Observer<T>.(T) -> Unit) : Observer<T> {

	init {
		lifecycleOwner.lifecycleScope.launch {
			suspendCancellableCoroutine<Unit> {
				it.invokeOnCancellation {
					this@LifeObserver.cancel()
				}
			}
		}
	}

	override val coroutineContext = parentContext + Dispatchers.Main.immediate

	private val waitingJob = SupervisorJob(coroutineContext[Job])

	private val waitingScope = this + waitingJob

	private fun isStarted() =
		lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

	override fun notify(value: T) {
		if (isStarted()) {
			onChange(value)
			return
		}
		waitingJob.cancelChildren()
		waitingScope.launch {
			onChange(suspendCancellableCoroutine { continuation ->
				val observer = onStartedObserver(value, continuation)
				continuation.invokeOnCancellation {
					lifecycleOwner.lifecycle.removeObserver(observer)
				}
				lifecycleOwner.lifecycle.addObserver(observer)
			})
		}
	}

	private fun onStartedObserver(value: T, continuation: Continuation<T>) =
		object : LifecycleEventObserver {
			override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
				if (event == Lifecycle.Event.ON_START) {
					source.lifecycle.removeObserver(this)
					continuation.resume(value)
				}
			}
		}
}
