package ru.yarsu.db

import java.sql.Connection

class UserDatabaseService(private val connection: Connection) {

    fun createUser(login: String, password: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val sql = "INSERT INTO users (id, login, password) VALUES (?, ?, ?)"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        statement.setString(2, login)
        statement.setString(3, password)
        statement.executeUpdate()
        return id
    }

    fun getUserById(id: String): User? {
        val sql = "SELECT id, login, password FROM users WHERE id = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, id)
        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val userId = resultSet.getString("id")
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
            val userId = resultSet.getString("id")
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
            val userId = resultSet.getString("id")
            val login = resultSet.getString("login")
            val password = resultSet.getString("password")
            users.add(User(userId, login, password))
        }
        return users
    }
}
