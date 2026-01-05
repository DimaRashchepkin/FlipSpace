package ru.yarsu

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import ru.yarsu.db.configureDatabases
import ru.yarsu.db.DatabaseFactory
import ru.yarsu.db.DatabaseService
import ru.yarsu.web.configureSerialization
import ru.yarsu.web.configureTemplating
import io.ktor.util.AttributeKey
import ru.yarsu.web.configureRouting


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


// Инициализация DatabaseService
fun Application.initDatabaseService() {
    try {
        // Создаем экземпляр DatabaseService
        val dbService = DatabaseService(DatabaseFactory.getConnection())
        // Сохраняем его в атрибутах приложения
        attributes.put(DatabaseServiceKey, dbService)
        println("Database service initialized successfully")
    } catch (e: Exception) {
        println("Failed to initialize database service: ${e.message}")
        // Создаем пустой сервис чтобы не было null
        attributes.put(DatabaseServiceKey, DatabaseService(DatabaseFactory.getConnection()))
    }
}

// Ключ для хранения DatabaseService в атрибутах приложения
val DatabaseServiceKey = AttributeKey<DatabaseService>("DatabaseService")

// Расширение для удобного доступа к DatabaseService
fun Application.getDatabaseService(): DatabaseService = attributes[DatabaseServiceKey]