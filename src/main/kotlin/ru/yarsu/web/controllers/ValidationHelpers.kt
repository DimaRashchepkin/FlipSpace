package ru.yarsu.web.controllers

data class ValidationError(val message: String)

object ValidationHelpers {
    fun validateRegistration(
        username: String,
        password: String,
        confirmPassword: String,
    ): ValidationError? {
        return when {
            username.isBlank() -> ValidationError("Логин не может быть пустым")
            password.isBlank() -> ValidationError("Пароль не может быть пустым")
            password != confirmPassword -> ValidationError("Пароли не совпадают")
            else -> null
        }
    }

    fun validateLogin(login: String, password: String): ValidationError? {
        return when {
            login.isBlank() || password.isBlank() ->
                ValidationError("Логин и пароль не могут быть пустыми")
            else -> null
        }
    }
}
