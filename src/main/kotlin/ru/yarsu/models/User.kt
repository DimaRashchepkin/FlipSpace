package ru.yarsu.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val login: String,
    @kotlinx.serialization.Transient
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
