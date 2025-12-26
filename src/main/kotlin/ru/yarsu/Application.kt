package ru.yarsu

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import ru.yarsu.db.configureDatabases
import ru.yarsu.web.configureSerialization
import ru.yarsu.web.configureTemplating

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureTemplating()
    initDatabaseService()
    configureRouting()
}

// Функция для инициализации сервиса базы данных
fun Application.initDatabaseService() {
    try {
        val dbService = DatabaseService(DatabaseFactory.getConnection())
        attributes.put(DatabaseServiceKey, dbService)
        println("Database service initialized successfully")
    } catch (e: Exception) {
        println("Failed to initialize database service: ${e.message}")
    }
}

// Ключ для хранения DatabaseService в атрибутах приложения
val DatabaseServiceKey = AttributeKey<DatabaseService>("DatabaseService")

// Расширение для удобного доступа к DatabaseService
fun Application.getDatabaseService(): DatabaseService = attributes[DatabaseServiceKey]
