package ru.yarsu.db

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.ArrayList

class User(val id: Int, val login: String, val password: String)

class Card(val id: Int, val authorId: Int, val content: String, val priority: Int)

class CardCreateRequest(val authorId: Int, val content: String, val priority: Int)

class CardSet(val id: String, val userId: String, val title: String, val description: String)

class DatabaseService(private val connection: Connection) {

    init {
        val statement = connection.createStatement()
        statement.execute("SELECT 1")
    }

    fun createUser(login: String, password: String): Int {
        val sql = "INSERT INTO users (login, password) VALUES (?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, login)
        statement.setString(2, password)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        return if (generatedKeys.next()) {
            generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted user")
        }
    }

    fun getUserById(id: Int): User? {
        val sql = "SELECT id, login, password FROM users WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val userId = resultSet.getInt("id")
            val userLogin = resultSet.getString("login")
            val userPassword = resultSet.getString("password")
            User(userId, userLogin, userPassword)
        } else {
            null
        }
    }

    fun getUserByLogin(login: String): User? {
        val sql = "SELECT id, login, password FROM users WHERE login = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, login)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val userId = resultSet.getInt("id")
            val userLogin = resultSet.getString("login")
            val userPassword = resultSet.getString("password")
            User(userId, userLogin, userPassword)
        } else {
            null
        }
    }

    fun getAllUsers(): List<User> {
        val sql = "SELECT id, login, password FROM users"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val users = ArrayList<User>()
        while (resultSet.next()) {
            val userId = resultSet.getInt("id")
            val login = resultSet.getString("login")
            val password = resultSet.getString("password")
            users.add(User(userId, login, password))
        }
        return users
    }

    fun createCard(cardRequest: CardCreateRequest): Int {
        val sql = "INSERT INTO cards (author_id, content, priority) VALUES (?, ?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setInt(1, cardRequest.authorId)
        statement.setString(2, cardRequest.content)
        statement.setInt(3, cardRequest.priority)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        return if (generatedKeys.next()) {
            generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted card")
        }
    }

    fun getCardById(id: Int): Card? {
        val sql = "SELECT id, author_id, content, priority FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, content, priority)
        } else {
            null
        }
    }

    fun getCardsByAuthor(authorId: Int): List<Card> {
        val sql = "SELECT id, author_id, content, priority FROM cards WHERE author_id = ? ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, authorId)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val cardAuthorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, cardAuthorId, content, priority))
        }
        return cards
    }

    fun getRandomCardByPriority(): Card? {
        val sql = "WITH weighted_cards AS ( " +
                "SELECT id, author_id, content, priority, " +
                "SUM(priority) OVER (ORDER BY id) as cumulative_weight " +
                "FROM cards " +
                ") " +
                "SELECT id, author_id, content, priority " +
                "FROM weighted_cards " +
                "WHERE cumulative_weight >= (SELECT random() * (SELECT SUM(priority) FROM cards)) " +
                "ORDER BY cumulative_weight " +
                "LIMIT 1"

        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, content, priority)
        } else {
            getFirstCard()
        }
    }

    private fun getFirstCard(): Card? {
        val sql = "SELECT id, author_id, content, priority FROM cards ORDER BY id LIMIT 1"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, content, priority)
        } else {
            null
        }
    }

    fun updateCard(id: Int, content: String, priority: Int): Boolean {
        val sql = "UPDATE cards SET content = ?, priority = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, content)
        statement.setInt(2, priority)
        statement.setInt(3, id)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }

    fun deleteCard(id: Int): Boolean {
        val sql = "DELETE FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }

    fun getAllCards(): List<Card> {
        val sql = "SELECT id, author_id, content, priority FROM cards ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, authorId, content, priority))
        }
        return cards
    }

    fun getCardSetsByUser(userId: String): List<CardSet> {
        val sql = "SELECT id, user_id, title, description FROM card_sets WHERE user_id = ? ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, userId)
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userId = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val description = resultSet.getString("description")
            cardSets.add(CardSet(id, userId, title, description))
        }
        return cardSets
    }

    fun getAllCardSets(): List<CardSet> {
        val sql = "SELECT id, user_id, title, description FROM card_sets ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userId = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val description = resultSet.getString("description")
            cardSets.add(CardSet(id, userId, title, description))
        }
        return cardSets
    }

    fun createCardSet(userId: String, title: String, description: String): String {
        val sql = "INSERT INTO card_sets (id, user_id, title, description) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql)

        val id = java.util.UUID.randomUUID().toString()
        statement.setString(1, id)
        statement.setString(2, userId)
        statement.setString(3, title)
        statement.setString(4, description)

        statement.executeUpdate()
        return id
    }
}