package com.example.runup.domain.model

sealed interface AuthResult<out T> {
    data class Success<T>(
        val data: T
    ) : AuthResult<T>

    data class Fail(
        val message: String,
        val throwable: Throwable? = null
    ) : AuthResult<Nothing>
}