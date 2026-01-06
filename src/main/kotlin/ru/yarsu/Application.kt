package ru.yarsu

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.util.AttributeKey
import ru.yarsu.db.DatabaseFactory
import ru.yarsu.db.DatabaseService
import ru.yarsu.db.configureDatabases
import ru.yarsu.web.configureRouting
import ru.yarsu.web.configureSerialization
import ru.yarsu.web.configureTemplating

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    initDatabaseService()
    configureTemplating()
    configureRouting()
}

fun Application.initDatabaseService() {
    try {
        val dbService = DatabaseService(DatabaseFactory.getConnection())
        attributes.put(DatabaseServiceKey, dbService)
        println("Database service initialized successfully")
    } catch (e: Exception) {
        println("Failed to initialize database service: ${e.message}")
        attributes.put(DatabaseServiceKey, DatabaseService(DatabaseFactory.getConnection()))
    }
}

val DatabaseServiceKey = AttributeKey<DatabaseService>("DatabaseService")

fun Application.getDatabaseService(): DatabaseService = attributes[DatabaseServiceKey]