package com.klitsie.scopedobserver

import kotlinx.coroutines.*

interface ExampleRepository {

	fun startObserving()

	fun stopObserving()

	companion object : ExampleRepository {
		private val job = SupervisorJob()

		private val coroutineScope = CoroutineScope(Dispatchers.IO) + job

		private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

		val someData = Observable<Result<String>>(Loading())

		override fun startObserving() {
			coroutineScope.launch {
				while (true) {
					delay(1000)
					val random = (1..10).map { kotlin.random.Random.nextInt(0, charPool.size) }
						.map(charPool::get).joinToString("")
					someData.value = Success(random)
				}
			}
		}

		override fun stopObserving() {
			job.cancel()
		}
	}
}
