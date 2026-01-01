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
}
