package ru.yarsu.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

@Serializable
data class User(val id: Int, val login: String, val password: String)

@Serializable
data class Card(val id: Int, val authorId: Int, val content: String, val priority: Int)

@Serializable
data class CardCreateRequest(val authorId: Int, val content: String, val priority: Int)

class DatabaseService(private val connection: Connection) {

    init {
        val statement = connection.createStatement()
        statement.execute("SELECT 1")
    }

    // User operations
    suspend fun createUser(login: String, password: String): Int = withContext(Dispatchers.IO) {
        val sql = "INSERT INTO users (login, password) VALUES (?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, login)
        statement.setString(2, password)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted user")
        }
    }

    suspend fun getUserById(id: Int): User? = withContext(Dispatchers.IO) {
        val sql = "SELECT id, login, password FROM users WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val userId = resultSet.getInt("id")
            val userLogin = resultSet.getString("login")
            val userPassword = resultSet.getString("password")
            return@withContext User(userId, userLogin, userPassword)
        } else {
            return@withContext null
        }
    }

    suspend fun getUserByLogin(login: String): User? = withContext(Dispatchers.IO) {
        val sql = "SELECT id, login, password FROM users WHERE login = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, login)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val userId = resultSet.getInt("id")
            val userLogin = resultSet.getString("login")
            val userPassword = resultSet.getString("password")
            return@withContext User(userId, userLogin, userPassword)
        } else {
            return@withContext null
        }
    }

    // Card operations
    suspend fun createCard(cardRequest: CardCreateRequest): Int = withContext(Dispatchers.IO) {
        val sql = "INSERT INTO cards (author_id, content, priority) VALUES (?, ?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setInt(1, cardRequest.authorId)
        statement.setString(2, cardRequest.content)
        statement.setInt(3, cardRequest.priority)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted card")
        }
    }

    suspend fun getCardById(id: Int): Card? = withContext(Dispatchers.IO) {
        val sql = "SELECT id, author_id, content, priority FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            return@withContext Card(cardId, authorId, content, priority)
        } else {
            return@withContext null
        }
    }

    suspend fun getCardsByAuthor(authorId: Int): List<Card> = withContext(Dispatchers.IO) {
        val sql = "SELECT id, author_id, content, priority FROM cards WHERE author_id = ? ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, authorId)
        val resultSet = statement.executeQuery()

        val cards = mutableListOf<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val cardAuthorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, cardAuthorId, content, priority))
        }
        return@withContext cards
    }

    suspend fun getRandomCardByPriority(): Card? = withContext(Dispatchers.IO) {
        // Выбор случайной карточки с учетом приоритета
        val sql = """
        WITH weighted_cards AS (
            SELECT id, author_id, content, priority,
                   SUM(priority) OVER (ORDER BY id) as cumulative_weight
            FROM cards
        )
        SELECT id, author_id, content, priority
        FROM weighted_cards
        WHERE cumulative_weight >= (SELECT random() * (SELECT SUM(priority) FROM cards))
        ORDER BY cumulative_weight
        LIMIT 1
    """.trimIndent()

        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            return@withContext Card(cardId, authorId, content, priority)
        } else {
            // Если запрос не вернул результатов, вернуть первую карточку
            return@withContext getFirstCard()
        }
    }

    private suspend fun getFirstCard(): Card? = withContext(Dispatchers.IO) {
        val sql = "SELECT id, author_id, content, priority FROM cards ORDER BY id LIMIT 1"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            return@withContext Card(cardId, authorId, content, priority)
        } else {
            return@withContext null
        }
    }

    suspend fun updateCard(id: Int, content: String, priority: Int): Boolean = withContext(Dispatchers.IO) {
        val sql = "UPDATE cards SET content = ?, priority = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, content)
        statement.setInt(2, priority)
        statement.setInt(3, id)
        val affectedRows = statement.executeUpdate()
        return@withContext affectedRows > 0
    }

    suspend fun deleteCard(id: Int): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val affectedRows = statement.executeUpdate()
        return@withContext affectedRows > 0
    }

    suspend fun getAllCards(): List<Card> = withContext(Dispatchers.IO) {
        val sql = "SELECT id, author_id, content, priority FROM cards ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cards = mutableListOf<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, authorId, content, priority))
        }
        return@withContext cards
    }
}