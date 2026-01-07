package ru.yarsu.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Card(
    val id: String = UUID.randomUUID().toString(),
    val setId: String,
    val title: String? = null,
    val frontText: String,
    val backText: String,
    val createdAt: Long = System.currentTimeMillis(),
)
