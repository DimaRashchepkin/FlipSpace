package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/*fun Application.configureSerialization() {
    routing {
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}*/

fun Application.configureSerialization() {
    // Установка плагина Content Negotiation с Kotlinx Serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Тестовый маршрут для проверки сериализации
    routing {
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }

        // Тестовый маршрут для проверки сериализации пользователей
        get("/json/test-user") {
            val testUser = UserResponse(1, "test_user")
            call.respond(ApiResponse(
                status = "success",
                message = "Test user data",
                data = testUser
            ))
        }

        // Тестовый маршрут для проверки сериализации карточек
        get("/json/test-card") {
            val testCard = CardResponse(1, 1, "Тестовая карточка", 2)
            call.respond(ApiResponse(
                status = "success",
                message = "Test card data",
                data = testCard
            ))
        }
    }
}