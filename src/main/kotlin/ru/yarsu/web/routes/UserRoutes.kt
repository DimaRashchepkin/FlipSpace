package ru.yarsu.web.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.yarsu.db.DatabaseService
import ru.yarsu.web.ApiResponse
import ru.yarsu.web.CreateUserResponse
import ru.yarsu.web.ErrorResponse
import ru.yarsu.web.UserResponse
import java.sql.SQLException

fun Route.userRoutes(dbService: DatabaseService) {
    route("/users") {
        get {
            handleGetAllUsers(dbService)
        }

        post("/create") {
            handleCreateUser(dbService)
        }

        get("/by-login/{login}") {
            handleGetUserByLogin(dbService)
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetAllUsers(dbService: DatabaseService) {
    try {
        val users = dbService.getAllUsers()
        val userResponses = users.map { user ->
            UserResponse(user.id, user.login)
        }
        call.respond(
            ApiResponse(
                status = "success",
                message = "Users retrieved successfully",
                data = userResponses,
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

// WARNING: This endpoint is for debugging only. In production, use /register endpoint instead.
// Passwords in query parameters are insecure and will be logged by web servers.
private suspend fun io.ktor.server.routing.RoutingContext.handleCreateUser(dbService: DatabaseService) {
    try {
        val parameters = call.request.queryParameters
        val login = parameters["login"] ?: "default_user"
        val password = parameters["password"] ?: "default_password"

        // This saves password as-is (plain text), which is INSECURE
        // Use AuthService.registerUser() instead for proper password hashing
        val userId = dbService.createUser(login, password)
        call.respond(
            CreateUserResponse(
                status = "success",
                userId = userId,
                login = login,
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetUserByLogin(dbService: DatabaseService) {
    try {
        val login = call.parameters["login"]
            ?: throw IllegalArgumentException("Login parameter is required")

        val user = dbService.getUserByLogin(login)

        if (user != null) {
            call.respond(
                ApiResponse(
                    status = "success",
                    message = "User found",
                    data = UserResponse(user.id, user.login),
                ),
            )
        } else {
            call.respond(
                ErrorResponse(
                    status = "not_found",
                    error = "User with login '$login' not found",
                ),
            )
        }
    } catch (e: IllegalArgumentException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = e.message ?: "Invalid argument",
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}
