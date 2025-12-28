package ru.yarsu.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class CardSet(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val description: String? = null,
    var content: List<Card> = emptyList(),
)
