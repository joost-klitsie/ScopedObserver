package com.klitsie.scopedobserver

sealed class Result<T> {

	abstract val value: T?

	abstract val error: Throwable?

	abstract val isSuccess: Boolean

	abstract val isLoading: Boolean

	abstract val isError: Boolean
}

data class Success<T>(
	override val value: T,
	override val error: Throwable? = null
) : Result<T>() {

	override val isSuccess = true
	override val isLoading = false
	override val isError = false
}

data class Error<T>(
	override val error: Throwable,
	override val value: T? = null
) : Result<T>() {

	override val isSuccess = true
	override val isLoading = false
	override val isError = false
}

data class Loading<T>(
	override val value: T? = null,
	override val error: Throwable? = null
) : Result<T>() {

	override val isSuccess = true
	override val isLoading = false
	override val isError = false
}

fun <T, K> Observable<Map<K, Result<T>>>.get(key: K): Result<T>? = value[key]

fun <T> Observable<Result<T>>.uniqueSuccess(newValue: T) {
	if (!value.isSuccess || newValue != value.value) {
		value = Success(newValue)
	}
}

fun <T> Observable<Result<T>>.uniqueNewValue(newValue: T) {
	val result = when (val oldValue = value) {
		is Success -> oldValue.copy(value = newValue)
		is Error -> oldValue.copy(value = newValue)
		is Loading -> oldValue.copy(value = newValue)
	}
	if (result != value) {
		value = result
	}
}

fun <T, R> Result<T>.mapTo(transform: (T) -> R): Result<R> =
	when (this) {
		is Success -> Success(value = transform(value))
		is Loading -> Loading(value = value?.let { transform(it) }, error = error)
		is Error -> Error(value = value?.let { transform(it) }, error = error)
	}