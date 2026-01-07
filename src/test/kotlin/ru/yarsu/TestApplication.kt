package ru.yarsu

import io.ktor.server.application.Application
import ru.yarsu.db.DatabaseService
import ru.yarsu.web.configureRouting
import ru.yarsu.web.configureSerialization
import ru.yarsu.web.configureSessions
import ru.yarsu.web.configureTemplating
import ru.yarsu.web.controllers.configureAuthRoutes
import java.sql.Connection
import java.sql.DriverManager

/**
 * Singleton object to hold shared test database connection
 */
object TestDatabaseConnection {
    private var _connection: Connection? = null

    fun getConnection(): Connection {
        if (_connection == null || _connection!!.isClosed) {
            _connection = createH2TestDatabase()
        }
        return _connection!!
    }

    private fun createH2TestDatabase(): Connection {
        val connection = DriverManager.getConnection(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            "sa",
            "",
        )

        // Initialize schema
        connection.createStatement().use { stmt ->
            try {
                stmt.execute("DROP TABLE IF EXISTS cards CASCADE")
                stmt.execute("DROP TABLE IF EXISTS card_sets CASCADE")
                stmt.execute("DROP TABLE IF EXISTS users CASCADE")

                stmt.execute(
                    """
                    CREATE TABLE users (
                        id VARCHAR(36) PRIMARY KEY,
                        login VARCHAR(50) UNIQUE NOT NULL,
                        password VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """.trimIndent(),
                )

                stmt.execute(
                    """
                    CREATE TABLE card_sets (
                        id VARCHAR(36) PRIMARY KEY,
                        user_id VARCHAR(36) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        is_private BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_card_sets_user
                            FOREIGN KEY (user_id)
                            REFERENCES users(id)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE
                    )
                    """.trimIndent(),
                )

                stmt.execute(
                    """
                    CREATE TABLE cards (
                        id VARCHAR(36) PRIMARY KEY,
                        set_id VARCHAR(36) NOT NULL,
                        front_text TEXT NOT NULL,
                        back_text TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_cards_set
                            FOREIGN KEY (set_id)
                            REFERENCES card_sets(id)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE
                    )
                    """.trimIndent(),
                )

                stmt.execute("CREATE INDEX idx_cards_set_id ON cards(set_id)")
                stmt.execute("CREATE INDEX idx_card_sets_user_id ON card_sets(user_id)")

                println("Test database schema created successfully")
            } catch (e: Exception) {
                println("Warning during schema creation (may be expected): ${e.message}")
            }
        }

        return connection
    }
}

/**
 * Test-specific application module that uses H2 in-memory database
 */
fun Application.testModule() {
    configureSerialization()
    configureTestDatabase()
    configureSessions()
    configureTemplating()
    configureAuthRoutes()
    configureRouting()
}

fun Application.configureTestDatabase() {
    try {
        val connection = TestDatabaseConnection.getConnection()
        val dbService = DatabaseService(connection)
        attributes.put(DatabaseServiceKey, dbService)
        println("Test database service initialized successfully with H2")
    } catch (e: Exception) {
        println("Failed to initialize test database service: ${e.message}")
        e.printStackTrace()
        throw e
    }
}
