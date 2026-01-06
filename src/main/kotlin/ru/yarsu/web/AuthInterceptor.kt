package ru.yarsu.web

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

suspend fun ApplicationCall.requireAuth(): UserSession? {
    val session = sessions.get<UserSession>()
    if (session == null) {
        respondRedirect("/login")
        return null
    }
    return session
}

suspend fun ApplicationCall.requireGuest(): Boolean {
    val session = sessions.get<UserSession>()
    if (session != null) {
        respondRedirect("/sets")
        return false
    }
    return true
}
