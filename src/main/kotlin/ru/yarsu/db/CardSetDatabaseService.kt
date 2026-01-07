package ru.yarsu.db

import ru.yarsu.DatabaseConstants
import java.sql.Connection

class CardSetDatabaseService(private val connection: Connection) {

    fun getCardSetsByUser(userId: String): List<CardSet> {
        val sql = "SELECT id, user_id, title, is_private FROM card_sets WHERE user_id = ? ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, userId)
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userIdFromDb = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val isPrivate = resultSet.getBoolean("is_private")
            cardSets.add(CardSet(id, userIdFromDb, title, isPrivate))
        }
        return cardSets
    }

    fun getAllCardSets(): List<CardSet> {
        val sql = "SELECT id, user_id, title, is_private FROM card_sets ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userIdFromDb = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val isPrivate = resultSet.getBoolean("is_private")
            cardSets.add(CardSet(id, userIdFromDb, title, isPrivate))
        }
        return cardSets
    }

    fun getAllCardSetsVisibleToUser(currentUserId: String?): List<CardSet> {
        val sql = if (currentUserId != null) {
            """
            SELECT id, user_id, title, is_private
            FROM card_sets
            WHERE is_private = false OR user_id = ?
            ORDER BY created_at DESC
            """.trimIndent()
        } else {
            "SELECT id, user_id, title, is_private FROM card_sets WHERE is_private = false ORDER BY created_at DESC"
        }

        val statement = connection.prepareStatement(sql)
        if (currentUserId != null) {
            statement.setString(1, currentUserId)
        }
        val resultSet = statement.executeQuery()

        val cardSets = ArrayList<CardSet>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val userIdFromDb = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val isPrivate = resultSet.getBoolean("is_private")
            cardSets.add(CardSet(id, userIdFromDb, title, isPrivate))
        }
        return cardSets
    }

    fun createCardSet(userId: String, title: String, isPrivate: Boolean = false): String {
        val id = java.util.UUID.randomUUID().toString()
        val sql = "INSERT INTO card_sets (id, user_id, title, is_private) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql)
        statement.setString(DatabaseConstants.FIRST_PARAMETER_INDEX, id)
        statement.setString(DatabaseConstants.SECOND_PARAMETER_INDEX, userId)
        statement.setString(DatabaseConstants.THIRD_PARAMETER_INDEX, title)
        statement.setBoolean(DatabaseConstants.FOURTH_PARAMETER_INDEX, isPrivate)

        statement.executeUpdate()
        return id
    }

    fun getCardsBySetId(setId: String): List<SetCard> {
        val sql = "SELECT id, set_id, title, front_text, back_text FROM cards WHERE set_id = ? ORDER BY created_at ASC"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, setId)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<SetCard>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val setIdFromDb = resultSet.getString("set_id")
            val title = resultSet.getString("title")
            val frontText = resultSet.getString("front_text")
            val backText = resultSet.getString("back_text")
            cards.add(SetCard(id, setIdFromDb, title, frontText, backText))
        }
        return cards
    }

    fun saveCardsForSet(setId: String, cards: List<SetCard>) {
        // Удаляем старые карточки
        val deleteSql = "DELETE FROM cards WHERE set_id = ?"
        val deleteStatement = connection.prepareStatement(deleteSql)
        deleteStatement.setString(1, setId)
        deleteStatement.executeUpdate()

        // Добавляем новые карточки
        if (cards.isNotEmpty()) {
            val insertSql = "INSERT INTO cards (id, set_id, title, front_text, back_text) VALUES (?, ?, ?, ?, ?)"
            val insertStatement = connection.prepareStatement(insertSql)

            for (card in cards) {
                insertStatement.setString(DatabaseConstants.FIRST_PARAMETER_INDEX, card.id)
                insertStatement.setString(DatabaseConstants.SECOND_PARAMETER_INDEX, card.setId)
                insertStatement.setString(DatabaseConstants.THIRD_PARAMETER_INDEX, card.title)
                insertStatement.setString(DatabaseConstants.FOURTH_PARAMETER_INDEX, card.frontText)
                insertStatement.setString(DatabaseConstants.FIFTH_PARAMETER_INDEX, card.backText)
                insertStatement.addBatch()
            }

            insertStatement.executeBatch()
        }
    }

    fun getCardSetById(id: String): CardSet? {
        val sql = "SELECT id, user_id, title, is_private FROM card_sets WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val setId = resultSet.getString("id")
            val userId = resultSet.getString("user_id")
            val title = resultSet.getString("title")
            val isPrivate = resultSet.getBoolean("is_private")
            CardSet(setId, userId, title, isPrivate)
        } else {
            null
        }
    }
}

class SetCard(val id: String, val setId: String, val title: String?, val frontText: String, val backText: String)
