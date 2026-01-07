package ru.yarsu.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CardSet(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val isPrivate: Boolean = false,
    var content: List<Card> = emptyList(),
)
