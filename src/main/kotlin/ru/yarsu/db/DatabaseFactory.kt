package ru.yarsu.db

import java.sql.Connection
import java.sql.DriverManager

object DatabaseFactory {
    fun getConnection(): Connection {
        val url = "jdbc:postgresql://localhost:5432/flipspace"
        val user = "flipspace_user"
        val password = "flipspace_password"

        return DriverManager.getConnection(url, user, password)
    }
}