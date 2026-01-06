package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import ru.yarsu.db.DatabaseFactory
import ru.yarsu.getDatabaseService
import ru.yarsu.web.routes.cardRoutes
import ru.yarsu.web.routes.cardSetRoutes
import ru.yarsu.web.routes.userRoutes
import java.sql.SQLException

@Serializable
data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null,
)

@Serializable
data class ErrorResponse(
    val status: String,
    val error: String,
)

@Serializable
data class UserResponse(val id: String, val login: String)

@Serializable
data class CardResponse(val id: String, val authorId: String, val content: String, val priority: Int)

@Serializable
data class CardSetResponse(val id: String, val userId: String, val title: String, val description: String)

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
    val timestamp: Long,
)

@Serializable
data class CreateUserResponse(val status: String, val userId: String, val login: String)

@Serializable
data class CreateCardResponse(val status: String, val cardId: String, val authorId: String)

@Serializable
data class CreateCardSetResponse(val status: String, val setId: String, val title: String)

fun Application.configureRouting() {
    val dbService = getDatabaseService()

    routing {
        get("/") {
            handleHomePage()
        }

        get("/health") {
            handleHealthCheck()
        }

        userRoutes(dbService)
        cardRoutes(dbService)
        cardSetRoutes(dbService)

        staticResources("/static", "static")
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleHomePage() {
    call.respondText(
        """
        <html>
        <head><title>FlipSpace API</title></head>
        <body>
            <h1>FlipSpace API</h1>
            <h2>Database Endpoints:</h2>
            <ul>
                <li><a href="/health">/health</a> - Проверка базы данных</li>
                <li><a href="/users">/users</a> - Все пользователи</li>
                <li><a href="/cards">/cards</a> - Все карточки</li>
                <li><a href="/cards/random">/cards/random</a> - Случайная карточка</li>
                <li><a href="/sets">/sets</a> - Наборы карточек текущего пользователя</li>
                <li><a href="/sets/by-user/12345">/sets/by-user/12345</a> - Наборы конкретного пользователя</li>
                <li><a href="/json/kotlinx-serialization">/json/kotlinx-serialization</a> - Тест сериализации</li>
            </ul>
        </body>
        </html>
        """.trimIndent(),
    )
}

private suspend fun io.ktor.server.routing.RoutingContext.handleHealthCheck() {
    try {
        val connection = DatabaseFactory.getConnection()
        val statement = connection.createStatement()
        statement.execute("SELECT 1")
        connection.close()

        call.respond(
            HealthResponse(
                status = "healthy",
                database = "connected",
                timestamp = System.currentTimeMillis(),
            ),
        )
    } catch (e: SQLException) {
        println("Health check failed: ${e.message}")
        call.respond(
            HealthResponse(
                status = "unhealthy",
                database = "disconnected: ${e.message}",
                timestamp = System.currentTimeMillis(),
            ),
        )
    }
}
