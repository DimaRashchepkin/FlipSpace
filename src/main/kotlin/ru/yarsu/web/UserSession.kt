package ru.yarsu.web

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: String,
    val username: String,
)
