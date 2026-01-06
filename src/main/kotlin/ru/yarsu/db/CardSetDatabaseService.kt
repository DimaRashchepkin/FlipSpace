package ru.yarsu.db

import ru.yarsu.DatabaseConstants
import java.sql.Connection

class CardSetDatabaseService(private val connection: Connection) {

    fun getCardSetsByUser(userId: String): List<CardSet> {
        val sql = "SELECT id, user_id, title, description FROM card_sets WHERE user_id = ? ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, userId)
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userIdFromDb = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val description = resultSet.getString("description")
            cardSets.add(CardSet(id, userIdFromDb, title, description))
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
            val userIdFromDb = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val description = resultSet.getString("description")
            cardSets.add(CardSet(id, userIdFromDb, title, description))
        }
        return cardSets
    }

    fun createCardSet(userId: String, title: String, description: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val sql = "INSERT INTO card_sets (id, user_id, title, description) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql)
        statement.setString(DatabaseConstants.FIRST_PARAMETER_INDEX, id)
        statement.setString(DatabaseConstants.SECOND_PARAMETER_INDEX, userId)
        statement.setString(DatabaseConstants.THIRD_PARAMETER_INDEX, title)
        statement.setString(DatabaseConstants.FOURTH_PARAMETER_INDEX, description)

        statement.executeUpdate()
        return id
    }
}
