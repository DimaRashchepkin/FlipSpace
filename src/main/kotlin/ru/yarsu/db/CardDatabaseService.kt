package ru.yarsu.db

import ru.yarsu.DatabaseConstants
import java.sql.Connection

class CardDatabaseService(private val connection: Connection) {

    fun createCard(cardRequest: CardCreateRequest): String {
        val id = java.util.UUID.randomUUID().toString()
        val sql = "INSERT INTO cards (id, author_id, content, priority) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql)
        statement.setString(DatabaseConstants.FIRST_PARAMETER_INDEX, id)
        statement.setString(DatabaseConstants.SECOND_PARAMETER_INDEX, cardRequest.authorId)
        statement.setString(DatabaseConstants.THIRD_PARAMETER_INDEX, cardRequest.content)
        statement.setInt(DatabaseConstants.FOURTH_PARAMETER_INDEX, cardRequest.priority)
        statement.executeUpdate()
        return id
    }

    fun getCardById(id: String): Card? {
        val sql = "SELECT id, author_id, content, priority FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getString("id")
            val authorId = resultSet.getString("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, content, priority)
        } else {
            null
        }
    }

    fun getCardsByAuthor(authorId: String): List<Card> {
        val sql =
            "SELECT id, author_id, content, priority FROM cards WHERE author_id = ? " +
                "ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, authorId)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getString("id")
            val cardAuthorId = resultSet.getString("author_id")
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
            val cardId = resultSet.getString("id")
            val authorId = resultSet.getString("author_id")
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
            val cardId = resultSet.getString("id")
            val authorId = resultSet.getString("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, content, priority)
        } else {
            null
        }
    }

    fun updateCard(id: String, content: String, priority: Int): Boolean {
        val sql = "UPDATE cards SET content = ?, priority = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(DatabaseConstants.FIRST_PARAMETER_INDEX, content)
        statement.setInt(DatabaseConstants.SECOND_PARAMETER_INDEX, priority)
        statement.setString(DatabaseConstants.THIRD_PARAMETER_INDEX, id)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }

    fun deleteCard(id: String): Boolean {
        val sql = "DELETE FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }

    fun getAllCards(): List<Card> {
        val sql = "SELECT id, author_id, content, priority FROM cards ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getString("id")
            val authorId = resultSet.getString("author_id")
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, authorId, content, priority))
        }
        return cards
    }
}
