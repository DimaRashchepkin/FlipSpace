package ru.yarsu.db

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.ArrayList

// Существующие классы
class User(val id: Int, val login: String, val password: String)
class Card(val id: Int, val authorId: Int, val bundleId: Int?, val content: String, val priority: Int) // Обновлено
class CardCreateRequest(val authorId: Int, val bundleId: Int?, val content: String, val priority: Int) // Обновлено

// Новые классы для наборов карточек
class Bundle(val id: Int, val name: String, val description: String?, val authorId: Int, val isPublic: Boolean)
class BundleCreateRequest(val name: String, val description: String?, val authorId: Int, val isPublic: Boolean = true)

class DatabaseService(private val connection: Connection) {

    init {
        val statement = connection.createStatement()
        statement.execute("SELECT 1")
    }

    // === ОПЕРАЦИИ С ПОЛЬЗОВАТЕЛЯМИ (без изменений) ===

    // User operations
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

    // === ОПЕРАЦИИ С НАБОРАМИ КАРТОЧЕК (НОВЫЕ) ===

    fun createBundle(bundleRequest: BundleCreateRequest): Int {
        val sql = "INSERT INTO bundles (name, description, author_id, is_public) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, bundleRequest.name)
        statement.setString(2, bundleRequest.description ?: "")
        statement.setInt(3, bundleRequest.authorId)
        statement.setBoolean(4, bundleRequest.isPublic)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        return if (generatedKeys.next()) {
            generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted bundle")
        }
    }

    fun getBundleById(id: Int): Bundle? {
        val sql = "SELECT id, name, description, author_id, is_public FROM bundles WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val bundleId = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val description = resultSet.getString("description")
            val authorId = resultSet.getInt("author_id")
            val isPublic = resultSet.getBoolean("is_public")
            Bundle(bundleId, name, description, authorId, isPublic)
        } else {
            null
        }
    }

    fun getBundlesByAuthor(authorId: Int): List<Bundle> {
        val sql = "SELECT id, name, description, author_id, is_public FROM bundles WHERE author_id = ? ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, authorId)
        val resultSet = statement.executeQuery()

        val bundles = ArrayList<Bundle>()
        while (resultSet.next()) {
            val bundleId = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val description = resultSet.getString("description")
            val bundleAuthorId = resultSet.getInt("author_id")
            val isPublic = resultSet.getBoolean("is_public")
            bundles.add(Bundle(bundleId, name, description, bundleAuthorId, isPublic))
        }
        return bundles
    }

    fun getPublicBundles(): List<Bundle> {
        val sql = "SELECT id, name, description, author_id, is_public FROM bundles WHERE is_public = TRUE ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val bundles = ArrayList<Bundle>()
        while (resultSet.next()) {
            val bundleId = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val description = resultSet.getString("description")
            val authorId = resultSet.getInt("author_id")
            val isPublic = resultSet.getBoolean("is_public")
            bundles.add(Bundle(bundleId, name, description, authorId, isPublic))
        }
        return bundles
    }

    fun updateBundle(id: Int, name: String, description: String?, isPublic: Boolean): Boolean {
        val sql = "UPDATE bundles SET name = ?, description = ?, is_public = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, name)
        statement.setString(2, description ?: "")
        statement.setBoolean(3, isPublic)
        statement.setInt(4, id)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }

    fun deleteBundle(id: Int): Boolean {
        // Удаляем карточки из набора (опционально - можно оставить без привязки)
        val clearCardsSql = "UPDATE cards SET bundle_id = NULL WHERE bundle_id = ?"
        val clearStatement = connection.prepareStatement(clearCardsSql)
        clearStatement.setInt(1, id)
        clearStatement.executeUpdate()

        // Удаляем сам набор
        val deleteBundleSql = "DELETE FROM bundles WHERE id = ?"
        val deleteStatement = connection.prepareStatement(deleteBundleSql)
        deleteStatement.setInt(1, id)
        val affectedRows = deleteStatement.executeUpdate()
        return affectedRows > 0
    }

    fun getAllBundles(): List<Bundle> {
        val sql = "SELECT id, name, description, author_id, is_public FROM bundles ORDER BY created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val bundles = ArrayList<Bundle>()
        while (resultSet.next()) {
            val bundleId = resultSet.getInt("id")
            val name = resultSet.getString("name")
            val description = resultSet.getString("description")
            val authorId = resultSet.getInt("author_id")
            val isPublic = resultSet.getBoolean("is_public")
            bundles.add(Bundle(bundleId, name, description, authorId, isPublic))
        }
        return bundles
    }

    // === ОПЕРАЦИИ С КАРТОЧКАМИ (ОБНОВЛЕННЫЕ) ===

    fun createCard(cardRequest: CardCreateRequest): Int {
        val sql = "INSERT INTO cards (author_id, bundle_id, content, priority) VALUES (?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        statement.setInt(1, cardRequest.authorId)
        if (cardRequest.bundleId != null) {
            statement.setInt(2, cardRequest.bundleId)
        } else {
            statement.setNull(2, java.sql.Types.INTEGER)
        }
        statement.setString(3, cardRequest.content)
        statement.setInt(4, cardRequest.priority)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        return if (generatedKeys.next()) {
            generatedKeys.getInt(1)
        } else {
            throw SQLException("Unable to retrieve the id of the newly inserted card")
        }
    }

    fun getCardById(id: Int): Card? {
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val bundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, bundleId, content, priority)
        } else {
            null
        }
    }

    fun getCardsByAuthor(authorId: Int): List<Card> {
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards WHERE author_id = ? ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, authorId)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val cardAuthorId = resultSet.getInt("author_id")
            val bundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, cardAuthorId, bundleId, content, priority))
        }
        return cards
    }

    fun getCardsByBundle(bundleId: Int): List<Card> {
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards WHERE bundle_id = ? ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, bundleId)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val cardBundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, authorId, cardBundleId, content, priority))
        }
        return cards
    }

    fun getRandomCardByPriority(bundleId: Int? = null): Card? {
        val baseSql = "WITH weighted_cards AS ( " +
                "SELECT id, author_id, bundle_id, content, priority, " +
                "SUM(priority) OVER (ORDER BY id) as cumulative_weight " +
                "FROM cards "

        val whereClause = if (bundleId != null) "WHERE bundle_id = ? " else ""

        val sql = baseSql + whereClause + ") " +
                "SELECT id, author_id, bundle_id, content, priority " +
                "FROM weighted_cards " +
                "WHERE cumulative_weight >= (SELECT random() * (SELECT SUM(priority) FROM cards " +
                (if (bundleId != null) "WHERE bundle_id = ?)" else ")") + " " +
                "ORDER BY cumulative_weight " +
                "LIMIT 1"

        val statement = connection.prepareStatement(sql)

        if (bundleId != null) {
            statement.setInt(1, bundleId)
            statement.setInt(2, bundleId)
        }

        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val cardBundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, cardBundleId, content, priority)
        } else {
            if (bundleId != null) {
                getFirstCardInBundle(bundleId)
            } else {
                getFirstCard()
            }
        }
    }

    private fun getFirstCardInBundle(bundleId: Int): Card? {
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards WHERE bundle_id = ? ORDER BY id LIMIT 1"
        val statement = connection.prepareStatement(sql)
        statement.setInt(1, bundleId)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val cardBundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, cardBundleId, content, priority)
        } else {
            null
        }
    }

    private fun getFirstCard(): Card? {
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards ORDER BY id LIMIT 1"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val bundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            Card(cardId, authorId, bundleId, content, priority)
        } else {
            null
        }
    }

    fun updateCard(id: Int, content: String, priority: Int, bundleId: Int? = null): Boolean {
        val sql = "UPDATE cards SET content = ?, priority = ?, bundle_id = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, content)
        statement.setInt(2, priority)
        if (bundleId != null) {
            statement.setInt(3, bundleId)
        } else {
            statement.setNull(3, java.sql.Types.INTEGER)
        }
        statement.setInt(4, id)
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
        val sql = "SELECT id, author_id, bundle_id, content, priority FROM cards ORDER BY priority DESC, created_at DESC"
        val statement = connection.prepareStatement(sql)
        val resultSet = statement.executeQuery()

        val cards = ArrayList<Card>()
        while (resultSet.next()) {
            val cardId = resultSet.getInt("id")
            val authorId = resultSet.getInt("author_id")
            val bundleId = resultSet.getObject("bundle_id") as Int?
            val content = resultSet.getString("content")
            val priority = resultSet.getInt("priority")
            cards.add(Card(cardId, authorId, bundleId, content, priority))
        }
        return cards
    }

    fun moveCardToBundle(cardId: Int, bundleId: Int?): Boolean {
        val sql = "UPDATE cards SET bundle_id = ? WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        if (bundleId != null) {
            statement.setInt(1, bundleId)
        } else {
            statement.setNull(1, java.sql.Types.INTEGER)
        }
        statement.setInt(2, cardId)
        val affectedRows = statement.executeUpdate()
        return affectedRows > 0
    }
}