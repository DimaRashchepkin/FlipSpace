package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import ru.yarsu.db.CardCreateRequest
import kotlinx.serialization.Serializable

/*fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")
    }
}*/

@Serializable
data class SimpleCreateUserRequest(val login: String, val password: String)

@Serializable
data class SimpleCreateCardRequest(val authorId: Int, val content: String, val priority: Int)

fun Application.configureRouting() {
    val dbService = getDatabaseService()

    routing {
        get("/") {
            call.respondText("""
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
                        <li><a href="/json/kotlinx-serialization">/json/kotlinx-serialization</a> - Тест сериализации</li>
                    </ul>
                </body>
                </html>
            """.trimIndent())
        }

        // Маршрут для проверки здоровья базы данных
        get("/health") {
            try {
                val connection = DatabaseFactory.getConnection()
                val statement = connection.createStatement()
                statement.execute("SELECT 1")
                connection.close()

                call.respond(HealthResponse(
                    status = "healthy",
                    database = "connected",
                    timestamp = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                call.respond(HealthResponse(
                    status = "unhealthy",
                    database = "disconnected",
                    timestamp = System.currentTimeMillis()
                ))
            }
        }

        // Маршруты для работы с пользователями
        route("/users") {
            // Получить всех пользователей
            get {
                try {
                    val users = dbService.getAllUsers()
                    // Конвертируем внутренние User в UserResponse
                    val userResponses = users.map { UserResponse(it.id, it.login) }
                    call.respond(ApiResponse(
                        status = "success",
                        message = "Users retrieved successfully",
                        data = userResponses
                    ))
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to get users: ${e.message}"
                    ))
                }
            }

            // Создать нового пользователя (через query parameters для простоты)
            post("/create") {
                try {
                    val parameters = call.request.queryParameters
                    val login = parameters["login"] ?: "default_user"
                    val password = parameters["password"] ?: "default_password"

                    val userId = dbService.createUser(login, password)
                    call.respond(CreateUserResponse(
                        status = "success",
                        userId = userId,
                        login = login
                    ))
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to create user: ${e.message}"
                    ))
                }
            }

            // Создать пользователя через JSON body
            post("/create-json") {
                try {
                    val request = call.receive<SimpleCreateUserRequest>()
                    val userId = dbService.createUser(request.login, request.password)
                    call.respond(CreateUserResponse(
                        status = "success",
                        userId = userId,
                        login = request.login
                    ))
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to create user: ${e.message}"
                    ))
                }
            }

            // Найти пользователя по логину
            get("/by-login/{login}") {
                try {
                    val login = call.parameters["login"]
                        ?: throw IllegalArgumentException("Login parameter is required")

                    val user = dbService.getUserByLogin(login)

                    if (user != null) {
                        call.respond(ApiResponse(
                            status = "success",
                            message = "User found",
                            data = UserResponse(user.id, user.login)
                        ))
                    } else {
                        call.respond(ErrorResponse(
                            status = "not_found",
                            error = "User with login '$login' not found"
                        ))
                    }
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to get user: ${e.message}"
                    ))
                }
            }
        }

        // Маршруты для работы с карточками
        route("/cards") {
            // Получить все карточки
            get {
                try {
                    val cards = dbService.getAllCards()
                    // Конвертируем внутренние Card в CardResponse
                    val cardResponses = cards.map { CardResponse(it.id, it.authorId, it.content, it.priority) }
                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "Cards retrieved successfully",
                            data = cardResponses
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get cards: ${e.message}"
                        )
                    )
                }
            }

            // Создать новую карточку (через query parameters)
            post("/create") {
                try {
                    val parameters = call.request.queryParameters
                    val authorId = parameters["authorId"]?.toInt() ?: 1
                    val content = parameters["content"] ?: "Sample card content"
                    val priority = parameters["priority"]?.toInt() ?: 1

                    val cardRequest = CardCreateRequest(authorId, content, priority)
                    val cardId = dbService.createCard(cardRequest)

                    call.respond(
                        CreateCardResponse(
                            status = "success",
                            cardId = cardId,
                            authorId = authorId
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to create card: ${e.message}"
                        )
                    )
                }
            }

            // Создать карточку через JSON body
            post("/create-json") {
                try {
                    val request = call.receive<SimpleCreateCardRequest>()
                    val cardRequest = CardCreateRequest(request.authorId, request.content, request.priority)
                    val cardId = dbService.createCard(cardRequest)

                    call.respond(
                        CreateCardResponse(
                            status = "success",
                            cardId = cardId,
                            authorId = request.authorId
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to create card: ${e.message}"
                        )
                    )
                }
            }

            // Получить случайную карточку (с учетом приоритета)
            get("/random") {
                try {
                    val randomCard = dbService.getRandomCardByPriority()

                    if (randomCard != null) {
                        call.respond(
                            ApiResponse(
                                status = "success",
                                message = "Random card retrieved",
                                data = CardResponse(
                                    randomCard.id,
                                    randomCard.authorId,
                                    randomCard.content,
                                    randomCard.priority
                                )
                            )
                        )
                    } else {
                        call.respond(
                            ErrorResponse(
                                status = "empty",
                                error = "No cards available in database"
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get random card: ${e.message}"
                        )
                    )
                }
            }

            // Получить карточки по автору
            get("/by-author/{authorId}") {
                try {
                    val authorId = call.parameters["authorId"]?.toInt()
                        ?: throw IllegalArgumentException("Author ID is required")

                    val cards = dbService.getCardsByAuthor(authorId)
                    val cardResponses = cards.map { CardResponse(it.id, it.authorId, it.content, it.priority) }

                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "Cards retrieved for author $authorId",
                            data = cardResponses
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get cards by author: ${e.message}"
                        )
                    )
                }
            }

            // Получить карточку по ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toInt()
                        ?: throw IllegalArgumentException("Card ID is required")

                    val card = dbService.getCardById(id)

                    if (card != null) {
                        call.respond(
                            ApiResponse(
                                status = "success",
                                message = "Card found",
                                data = CardResponse(card.id, card.authorId, card.content, card.priority)
                            )
                        )
                    } else {
                        call.respond(
                            ErrorResponse(
                                status = "not_found",
                                error = "Card with ID $id not found"
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get card: ${e.message}"
                        )
                    )
                }
            }
            get("/from-bundle/{bundleId}/random") {
                try {
                    val bundleId = call.parameters["bundleId"]?.toInt()
                        ?: throw IllegalArgumentException("Bundle ID is required")

                    val randomCard = dbService.getRandomCardByPriority(bundleId)

                    if (randomCard != null) {
                        call.respond(ApiResponse(
                            status = "success",
                            message = "Random card from bundle $bundleId",
                            data = CardResponse(randomCard.id, randomCard.authorId, randomCard.bundleId, randomCard.content, randomCard.priority)
                        ))
                    } else {
                        call.respond(ErrorResponse(
                            status = "empty",
                            error = "No cards available in bundle $bundleId"
                        ))
                    }
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to get random card from bundle: ${e.message}"
                    ))
                }
            }

// Переместить карточку в другой набор
            post("/{id}/move") {
                try {
                    val cardId = call.parameters["id"]?.toInt()
                        ?: throw IllegalArgumentException("Card ID is required")

                    val parameters = call.request.queryParameters
                    val bundleId = parameters["bundleId"]?.toInt()

                    val success = dbService.moveCardToBundle(cardId, bundleId)

                    if (success) {
                        call.respond(ApiResponse(
                            status = "success",
                            message = "Card moved successfully",
                            data = mapOf("card_id" to cardId, "bundle_id" to bundleId)
                        ))
                    } else {
                        call.respond(ErrorResponse(
                            status = "not_found",
                            error = "Card with ID $cardId not found"
                        ))
                    }
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to move card: ${e.message}"
                    ))
                }
            }

// Получить карточки из набора
            get("/from-bundle/{bundleId}") {
                try {
                    val bundleId = call.parameters["bundleId"]?.toInt()
                        ?: throw IllegalArgumentException("Bundle ID is required")

                    val cards = dbService.getCardsByBundle(bundleId)
                    call.respond(ApiResponse(
                        status = "success",
                        message = "Cards from bundle $bundleId",
                        data = cards.map {
                            CardResponse(it.id, it.authorId, it.bundleId, it.content, it.priority)
                        }
                    ))
                } catch (e: Exception) {
                    call.respond(ErrorResponse(
                        status = "error",
                        error = "Failed to get cards from bundle: ${e.message}"
                    ))
                }
            }
        }

// Маршруты для работы с наборами карточек
            route("/bundles") {
                // Получить все наборы
                get {
                    try {
                        val bundles = dbService.getAllBundles()
                        call.respond(ApiResponse(
                            status = "success",
                            message = "Bundles retrieved successfully",
                            data = bundles.map {
                                BundleResponse(it.id, it.name, it.description, it.authorId, it.isPublic)
                            }
                        ))
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to get bundles: ${e.message}"
                        ))
                    }
                }

                // Создать новый набор
                post("/create") {
                    try {
                        val parameters = call.request.queryParameters
                        val name = parameters["name"] ?: "New Bundle"
                        val description = parameters["description"]
                        val authorId = parameters["authorId"]?.toInt() ?: 1
                        val isPublic = parameters["isPublic"]?.toBoolean() ?: true

                        val bundleRequest = BundleCreateRequest(name, description, authorId, isPublic)
                        val bundleId = dbService.createBundle(bundleRequest)

                        call.respond(ApiResponse(
                            status = "success",
                            message = "Bundle created successfully",
                            data = mapOf("bundle_id" to bundleId, "name" to name)
                        ))
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to create bundle: ${e.message}"
                        ))
                    }
                }

                // Получить набор по ID
                get("/{id}") {
                    try {
                        val id = call.parameters["id"]?.toInt()
                            ?: throw IllegalArgumentException("Bundle ID is required")

                        val bundle = dbService.getBundleById(id)

                        if (bundle != null) {
                            // Получаем карточки в этом наборе
                            val cards = dbService.getCardsByBundle(id)
                            call.respond(ApiResponse(
                                status = "success",
                                message = "Bundle found",
                                data = mapOf(
                                    "bundle" to BundleResponse(
                                        bundle.id,
                                        bundle.name,
                                        bundle.description,
                                        bundle.authorId,
                                        bundle.isPublic,
                                        cards.size
                                    ),
                                    "cards" to cards.map {
                                        CardResponse(it.id, it.authorId, it.bundleId, it.content, it.priority)
                                    }
                                )
                            ))
                        } else {
                            call.respond(ErrorResponse(
                                status = "not_found",
                                error = "Bundle with ID $id not found"
                            ))
                        }
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to get bundle: ${e.message}"
                        ))
                    }
                }

                // Получить наборы по автору
                get("/by-author/{authorId}") {
                    try {
                        val authorId = call.parameters["authorId"]?.toInt()
                            ?: throw IllegalArgumentException("Author ID is required")

                        val bundles = dbService.getBundlesByAuthor(authorId)
                        call.respond(ApiResponse(
                            status = "success",
                            message = "Bundles retrieved for author $authorId",
                            data = bundles.map {
                                BundleResponse(it.id, it.name, it.description, it.authorId, it.isPublic)
                            }
                        ))
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to get bundles by author: ${e.message}"
                        ))
                    }
                }

                // Получить публичные наборы
                get("/public") {
                    try {
                        val bundles = dbService.getPublicBundles()
                        call.respond(ApiResponse(
                            status = "success",
                            message = "Public bundles retrieved",
                            data = bundles.map {
                                BundleResponse(it.id, it.name, it.description, it.authorId, it.isPublic)
                            }
                        ))
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to get public bundles: ${e.message}"
                        ))
                    }
                }

                // Обновить набор
                put("/{id}") {
                    try {
                        val id = call.parameters["id"]?.toInt()
                            ?: throw IllegalArgumentException("Bundle ID is required")

                        val parameters = call.request.queryParameters
                        val name = parameters["name"] ?: throw IllegalArgumentException("Name is required")
                        val description = parameters["description"]
                        val isPublic = parameters["isPublic"]?.toBoolean() ?: true

                        val success = dbService.updateBundle(id, name, description, isPublic)

                        if (success) {
                            call.respond(ApiResponse(
                                status = "success",
                                message = "Bundle updated successfully",
                                data = mapOf("bundle_id" to id)
                            ))
                        } else {
                            call.respond(ErrorResponse(
                                status = "not_found",
                                error = "Bundle with ID $id not found"
                            ))
                        }
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to update bundle: ${e.message}"
                        ))
                    }
                }

                // Удалить набор
                delete("/{id}") {
                    try {
                        val id = call.parameters["id"]?.toInt()
                            ?: throw IllegalArgumentException("Bundle ID is required")

                        val success = dbService.deleteBundle(id)

                        if (success) {
                            call.respond(ApiResponse(
                                status = "success",
                                message = "Bundle deleted successfully",
                                data = mapOf("bundle_id" to id)
                            ))
                        } else {
                            call.respond(ErrorResponse(
                                status = "not_found",
                                error = "Bundle with ID $id not found"
                            ))
                        }
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            status = "error",
                            error = "Failed to delete bundle: ${e.message}"
                        ))
                    }
                }
            }




        // Статические ресурсы
        staticResources("/static", "static")
    }
}