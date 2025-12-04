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

fun Application.configureTemplating() {
    install(Pebble) {
        loader(
            ClasspathLoader().apply {
                prefix = "templates"
            },
        )
    }
    routing {
        staticResources("/static", "static")
        get("/") {
            call.respond(PebbleContent("common/front.html", emptyMap()))
        }
        get("/login") {
            call.respond(PebbleContent("authentication/login.html", emptyMap()))
        }
        get("/register") {
            call.respond(PebbleContent("authentication/register.html", emptyMap()))
        }
        get("/forgot-password") {
            call.respond(PebbleContent("authentication/forgot-password.html", emptyMap()))
        }
        get("/reset-password") {
            call.respond(PebbleContent("authentication/reset-password.html", emptyMap()))
        }
    }
}
