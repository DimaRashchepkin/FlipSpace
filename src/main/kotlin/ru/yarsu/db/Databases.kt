package ru.yarsu.db

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import java.sql.Connection
import java.sql.DriverManager

fun Application.configureDatabases() {
    val dbConnection: Connection = connectToPostgres(embedded = true)
    val cityService = CityService(dbConnection)

    routing {
        configureCityRoutes(cityService)
    }
}

private fun Route.configureCityRoutes(cityService: CityService) {
    createCityRoute(cityService)
    readCityRoute(cityService)
    updateCityRoute(cityService)
    deleteCityRoute(cityService)
}

private fun Route.createCityRoute(cityService: CityService) {
    post("/cities") {
        val city = call.receive<City>()
        val id = cityService.create(city)
        call.respond(HttpStatusCode.Created, id)
    }
}

@Suppress("SwallowedException")
private fun Route.readCityRoute(cityService: CityService) {
    get("/cities/{id}") {
        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
        try {
            val city = cityService.read(id)
            call.respond(HttpStatusCode.OK, city)
        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, "City not found")
        }
    }
}

@Suppress("SwallowedException")
private fun Route.updateCityRoute(cityService: CityService) {
    put("/cities/{id}") {
        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
        val user = call.receive<City>()
        try {
            cityService.update(id, user)
            call.respond(HttpStatusCode.OK)
        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, "City not found")
        }
    }
}

@Suppress("SwallowedException")
private fun Route.deleteCityRoute(cityService: CityService) {
    delete("/cities/{id}") {
        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
        try {
            cityService.delete(id)
            call.respond(HttpStatusCode.OK)
        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, "City not found")
        }
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    if (embedded) {
        log.info("Using embedded H2 database for testing; replace this flag to use postgres")
        return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        log.info("Connecting to postgres database at $url")
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        return DriverManager.getConnection(url, user, password)
    }
}
