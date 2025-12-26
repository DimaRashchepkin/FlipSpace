package ru.yarsu.web

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(val status: String, val message: String, val data: Any? = null)

@Serializable
data class ErrorResponse(val status: String, val error: String)

@Serializable
data class UserResponse(val id: Int, val login: String)

@Serializable
data class CardResponse(val id: Int, val authorId: Int, val content: String, val priority: Int)

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
    val timestamp: Long
)

@Serializable
data class CreateUserRequest(val login: String, val password: String)

@Serializable
data class CreateUserResponse(val status: String, val userId: Int, val login: String)

@Serializable
data class CreateCardRequest(val authorId: Int, val content: String, val priority: Int)

@Serializable
data class CreateCardResponse(val status: String, val cardId: Int, val authorId: Int)

@Serializable
data class BundleResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val authorId: Int,
    val isPublic: Boolean,
    val cardCount: Int? = null // Опционально: количество карточек в наборе
)

@Serializable
data class BundleCreateRequest(
    val name: String,
    val description: String? = null,
    val authorId: Int,
    val isPublic: Boolean = true
)

@Serializable
data class BundleUpdateRequest(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean
)

@Serializable
data class CardResponse(
    val id: Int,
    val authorId: Int,
    val bundleId: Int?, // Обновлено
    val content: String,
    val priority: Int
)

@Serializable
data class CreateCardRequest(
    val authorId: Int,
    val bundleId: Int? = null, // Обновлено
    val content: String,
    val priority: Int = 1
)

@Serializable
data class MoveCardRequest(
    val cardId: Int,
    val bundleId: Int?
)