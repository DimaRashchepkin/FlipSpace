package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.pebble.Pebble
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.pebbletemplates.pebble.loader.ClasspathLoader
import ru.yarsu.getDatabaseService
import ru.yarsu.services.TestCardSetService
import ru.yarsu.web.controllers.CardSetController

fun Application.configureTemplating() {
    install(Pebble) {
        loader(
            ClasspathLoader().apply {
                prefix = "templates"
            },
        )
    }

    val databaseService = getDatabaseService()
    val cardSetService = TestCardSetService(databaseService)
    val cardSetController = CardSetController(cardSetService)

    routing {
        staticResources("/static", "static")

        get("/") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondRedirect("/sets")
            } else {
                call.respond(PebbleContent("common/front.html", mapOf()))
            }
        }

        cardSetController.configureRoutes(this)

        get("/forgot-password") {
            call.respond(PebbleContent("authentication/forgot-password.html", mapOf()))
        }

        get("/reset-password") {
            call.respond(PebbleContent("authentication/reset-password.html", mapOf()))
        }
    }
}
