package ru.yarsu.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: Int,
    val login: String,
    @kotlinx.serialization.Transient
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)