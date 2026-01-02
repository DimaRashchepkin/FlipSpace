package ru.yarsu.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val login: String,
    @Transient
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
