package ru.yarsu.db

import java.sql.Connection

class User(val id: String, val login: String, val password: String)
class Card(val id: String, val authorId: String, val content: String, val priority: Int)
class CardCreateRequest(val authorId: String, val content: String, val priority: Int)
class CardSet(val id: String, val userId: String, val title: String, val isPrivate: Boolean = false)

class DatabaseService(connection: Connection) {

    val users = UserDatabaseService(connection)
    val cards = CardDatabaseService(connection)
    val cardSets = CardSetDatabaseService(connection)

    init {
        val statement = connection.createStatement()
        statement.execute("SELECT 1")
    }

    // Delegate to specialized services - users can also access services directly via public properties
    fun createUser(login: String, password: String): String = users.createUser(login, password)
    fun getUserById(id: String): User? = users.getUserById(id)
    fun getUserByLogin(login: String): User? = users.getUserByLogin(login)
    fun getAllUsers(): List<User> = users.getAllUsers()
    fun createCard(cardRequest: CardCreateRequest): String = cards.createCard(cardRequest)
    fun getCardById(id: String): Card? = cards.getCardById(id)
    fun getCardsByAuthor(authorId: String): List<Card> = cards.getCardsByAuthor(authorId)
    fun getRandomCardByPriority(): Card? = cards.getRandomCardByPriority()
    fun getAllCards(): List<Card> = cards.getAllCards()
    fun getCardSetsByUser(userId: String): List<CardSet> = cardSets.getCardSetsByUser(userId)
    fun getAllCardSets(): List<CardSet> = cardSets.getAllCardSets()
}
