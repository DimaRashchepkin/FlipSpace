package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.pebble.Pebble
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
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
            call.respond(PebbleContent("common/front.html", mapOf()))
        }

        cardSetController.configureRoutes(this)

        get("/login") {
            call.respond(PebbleContent("authentication/login.html", mapOf()))
        }

        get("/register") {
            call.respond(PebbleContent("authentication/register.html", mapOf()))
        }

        get("/forgot-password") {
            call.respond(PebbleContent("authentication/forgot-password.html", mapOf()))
        }

        get("/reset-password") {
            call.respond(PebbleContent("authentication/reset-password.html", mapOf()))
        }
    }
}
