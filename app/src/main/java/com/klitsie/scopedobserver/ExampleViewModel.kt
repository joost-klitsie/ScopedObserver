package com.klitsie.scopedobserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class ExampleViewModel : ViewModel() {

	private val repository = ExampleRepository

	val data = Observable(ExampleModel(null, false))

	init {
		repository.someData.observe(viewModelScope) {
			when (it) {
				is Success -> data.value = data.value.copy(text = it.value, isLoading = false)
				is Loading -> data.value = data.value.copy(isLoading = true)
				is Error -> data.value =
					data.value.copy(text = "Oh noes an error", isLoading = false)
			}
		}
	}

	fun onResume() {
		repository.startObserving()
	}

	fun onPause() {
		repository.stopObserving()
	}

}