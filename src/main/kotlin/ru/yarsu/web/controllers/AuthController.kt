package ru.yarsu.web.controllers

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import ru.yarsu.getDatabaseService
import ru.yarsu.services.AuthService
import ru.yarsu.web.UserSession
import ru.yarsu.web.requireGuest

fun Application.configureAuthRoutes() {
    val dbService = getDatabaseService()
    val authService = AuthService(dbService)

    routing {
        get("/register") {
            if (!call.requireGuest()) return@get
            call.respond(PebbleContent("authentication/register.html", mapOf<String, Any>()))
        }

        post("/register") {
            handleRegisterPost(authService)
        }

        get("/login") {
            if (!call.requireGuest()) return@get
            call.respond(PebbleContent("authentication/login.html", mapOf<String, Any>()))
        }

        post("/login") {
            handleLoginPost(authService)
        }

        get("/logout") {
            handleLogout()
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleRegisterPost(authService: AuthService) {
    val params = call.receiveParameters()
    val username = params["username"]?.trim() ?: ""
    val password = params["password"]?.trim() ?: ""
    val confirmPassword = params["confirm_password"]?.trim() ?: ""

    val validationError = ValidationHelpers.validateRegistration(username, password, confirmPassword)
    if (validationError != null) {
        call.respond(
            io.ktor.http.HttpStatusCode.BadRequest,
            PebbleContent(
                "authentication/register.html",
                mapOf("error" to validationError.message, "username" to username),
            ),
        )
        return
    }

    val result = authService.registerUser(username, password)
    result.fold(
        onSuccess = { userId ->
            call.sessions.set(UserSession(userId = userId, username = username))
            call.respondRedirect("/sets")
        },
        onFailure = { error ->
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                PebbleContent(
                    "authentication/register.html",
                    mapOf("error" to (error.message ?: ""), "username" to username),
                ),
            )
        },
    )
}

private suspend fun io.ktor.server.routing.RoutingContext.handleLoginGet() {
    call.respond(PebbleContent("authentication/login.html", mapOf<String, Any>()))
}

private suspend fun io.ktor.server.routing.RoutingContext.handleLoginPost(authService: AuthService) {
    val params = call.receiveParameters()
    val login = params["login"]?.trim() ?: ""
    val password = params["password"]?.trim() ?: ""

    val validationError = ValidationHelpers.validateLogin(login, password)
    if (validationError != null) {
        call.respond(
            io.ktor.http.HttpStatusCode.BadRequest,
            PebbleContent(
                "authentication/login.html",
                mapOf("error" to validationError.message, "login" to login),
            ),
        )
        return
    }

    val result = authService.authenticateUser(login, password)
    result.fold(
        onSuccess = { user ->
            call.sessions.set(UserSession(userId = user.id, username = user.login))
            call.respondRedirect("/sets")
        },
        onFailure = { error ->
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                PebbleContent(
                    "authentication/login.html",
                    mapOf("error" to (error.message ?: ""), "login" to login),
                ),
            )
        },
    )
}

private suspend fun io.ktor.server.routing.RoutingContext.handleLogout() {
    call.sessions.clear<UserSession>()
    call.respondRedirect("/login")
}
