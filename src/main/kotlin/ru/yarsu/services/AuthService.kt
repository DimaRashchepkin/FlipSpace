package ru.yarsu.services

import at.favre.lib.crypto.bcrypt.BCrypt
import ru.yarsu.AuthConstants
import ru.yarsu.db.DatabaseService
import ru.yarsu.db.User
import java.sql.SQLException

class AuthService(private val dbService: DatabaseService) {

    companion object {
        private const val BCRYPT_COST = 12
    }

    fun registerUser(login: String, password: String): Result<String> {
        return try {
            validateAndRegisterUser(login, password)
        } catch (e: SQLException) {
            Result.failure(Exception("Ошибка базы данных при регистрации: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            Result.failure(Exception("Недопустимые параметры регистрации: ${e.message}"))
        }
    }

    private fun validateAndRegisterUser(login: String, password: String): Result<String> {
        val validationError = validateRegistrationCredentials(login, password)
        if (validationError != null) {
            return Result.failure(Exception(validationError))
        }

        val existingUser = dbService.getUserByLogin(login)
        if (existingUser != null) {
            return Result.failure(Exception("Пользователь с таким логином уже существует"))
        }

        val hashedPassword = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
        val userId = dbService.createUser(login, hashedPassword)

        return Result.success(userId)
    }

    fun authenticateUser(login: String, password: String): Result<User> {
        return try {
            val user = dbService.getUserByLogin(login)
            val authResult = when {
                user == null -> Result.failure(Exception("Неверный логин или пароль"))
                !BCrypt.verifyer().verify(password.toCharArray(), user.password).verified ->
                    Result.failure(Exception("Неверный логин или пароль"))
                else -> Result.success(user)
            }
            authResult
        } catch (e: SQLException) {
            Result.failure(Exception("Ошибка базы данных при аутентификации: ${e.message}"))
        } catch (e: IllegalStateException) {
            Result.failure(Exception("Ошибка состояния при аутентификации: ${e.message}"))
        }
    }

    fun validateSession(userId: String): User? {
        return try {
            dbService.getUserById(userId)
        } catch (e: SQLException) {
            println("Database error during session validation: ${e.message}")
            null
        }
    }

    private fun validateRegistrationCredentials(login: String, password: String): String? {
        return when {
            login.isBlank() || login.length < AuthConstants.MIN_USERNAME_LENGTH ->
                "Логин должен содержать минимум ${AuthConstants.MIN_USERNAME_LENGTH} символа"
            password.isBlank() || password.length < AuthConstants.MIN_PASSWORD_LENGTH ->
                "Пароль должен содержать минимум ${AuthConstants.MIN_PASSWORD_LENGTH} символов"
            else -> null
        }
    }
}
