package com.pixelus.music.data.result

sealed class Result<out T, out E> {
    data class Success<T>(val data: T) : Result<T, Nothing>()
    data class Error<E>(val error: E) : Result<Nothing, E>()
}

sealed class DataError {
    data object NoReadPermission : DataError()
    data object FailedToRead : DataError()
    data object NoWritePermission : DataError()
    data object FailedToWrite : DataError()
    data object FileNotFound : DataError()
}
