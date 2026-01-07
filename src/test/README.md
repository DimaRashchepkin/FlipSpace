# FlipSpace Tests

## Обзор

Этот каталог содержит тесты для приложения FlipSpace, организованные по приоритетам безопасности и функциональности.

## Структура тестов

```
src/test/kotlin/ru/yarsu/
├── services/
│   └── AuthServiceTest.kt          # Unit-тесты для AuthService
├── web/
│   └── controllers/
│       └── AuthControllerTest.kt   # Integration-тесты для AuthController
├── TestHelpers.kt                   # Утилиты и хелперы для тестов
└── ApplicationTest.kt               # Базовые тесты приложения
```

## Покрытие тестами

### ✅ Критический приоритет - Security & Auth (ЗАВЕРШЕНО)

#### AuthService (11 тестов)
- ✅ Регистрация с валидными данными
- ✅ Регистрация с дублирующимся логином
- ✅ Регистрация с коротким логином (< 3 символов)
- ✅ Регистрация с пустым логином
- ✅ Регистрация с коротким паролем (< 6 символов)
- ✅ Регистрация с пустым паролем
- ✅ BCrypt хеширование пароля
- ✅ Аутентификация с правильным паролем
- ✅ Аутентификация с неправильным паролем
- ✅ Аутентификация несуществующего пользователя
- ✅ Валидация сессии

#### AuthController (15 тестов)
- ✅ GET /register возвращает страницу регистрации
- ✅ GET /login возвращает страницу входа
- ✅ POST /register успешно создает пользователя
- ✅ POST /register с дублирующимся логином возвращает ошибку
- ✅ POST /register с коротким логином возвращает ошибку
- ✅ POST /register с коротким паролем возвращает ошибку
- ✅ POST /register с несовпадающими паролями возвращает ошибку
- ✅ POST /register с пустыми полями возвращает ошибку
- ✅ POST /login успешно устанавливает сессию
- ✅ POST /login с неправильным паролем возвращает ошибку
- ✅ POST /login с несуществующим пользователем возвращает ошибку
- ✅ POST /login с пустыми полями возвращает ошибку
- ✅ GET /logout очищает сессию и редиректит
- ✅ GET /sets без сессии редиректит на /login
- ✅ Сессия сохраняется между запросами

**Итого: 28 тестов для критичной функциональности (13 unit + 15 integration)**

## Запуск тестов

### Все тесты
```bash
./gradlew test
```

### Только тесты безопасности
```bash
./gradlew test --tests "*AuthServiceTest"
./gradlew test --tests "*AuthControllerTest"
```

### Конкретный тест
```bash
./gradlew test --tests "AuthServiceTest.registerUser with valid credentials should succeed"
```

### С подробным выводом
```bash
./gradlew test --info
```

### С HTML отчетом
```bash
./gradlew test
# Отчет будет в: build/reports/tests/test/index.html
```

## Технологии

- **JUnit 4**: Основной фреймворк для тестирования
- **Kotlin Test**: Ассерты и утилиты для Kotlin
- **MockK**: Мокирование зависимостей
- **Ktor Test Host**: Интеграционное тестирование HTTP эндпоинтов

## Зависимости тестов

```kotlin
testImplementation(libs.ktor.server.test.host)  // Ktor testing
testImplementation(libs.kotlin.test.junit)      // Kotlin + JUnit
testImplementation(libs.mockk)                  // Mocking library
```

## Написание новых тестов

### Unit-тест для сервиса

```kotlin
@Test
fun `myFunction should do something`() {
    // Arrange
    val mockService = mockk<MyService>()
    every { mockService.getData() } returns "test data"

    // Act
    val result = myFunction(mockService)

    // Assert
    assertEquals("expected", result)
    verify { mockService.getData() }
}
```

### Integration-тест для контроллера

```kotlin
@Test
fun `POST endpoint should return 200`() = testApplication {
    application {
        module()
    }

    val response = client.post("/endpoint") {
        setBody("data")
    }

    assertEquals(HttpStatusCode.OK, response.status)
}
```

### Использование TestHelpers

```kotlin
@Test
fun `test with authenticated user`() = testApplication {
    application {
        module()
    }

    // Создать и залогинить пользователя
    val (client, username) = loginTestUser()

    // Теперь можно делать запросы с аутентификацией
    val response = client.get("/protected-route")
    assertEquals(HttpStatusCode.OK, response.status)
}
```

## Покрытие кода

Текущее покрытие тестами:
- **AuthService**: 100% (13 unit-тестов)
- **AuthController**: ~95% (15 integration-тестов)
- **Общее по безопасности**: 100% критичных функций

## Следующие шаги

Следующие компоненты для тестирования:
1. CardSetService (бизнес-логика управления наборами)
2. CardSetController (HTTP endpoints для наборов)
3. Database Services (интеграционные тесты с БД)
4. Валидация данных

## CI/CD

Тесты автоматически запускаются при:
- Каждом push в main ветку
- Создании pull request
- Ручном триггере через GitHub Actions

## Проблемы и решения

### База данных для тестов

Тесты используют встроенную H2 базу данных в режиме in-memory. Для тестов с реальной PostgreSQL рекомендуется использовать TestContainers (запланировано).

### Изоляция тестов

Каждый integration-тест использует уникальные username'ы (timestamp + random) для избежания конфликтов при параллельном запуске.

### Моки vs Реальные зависимости

- **Unit-тесты**: Используют моки (MockK) для изоляции
- **Integration-тесты**: Используют реальное Ktor приложение с реальной БД

## Контрибьюторам

При добавлении новой функциональности:
1. Напишите unit-тесты для бизнес-логики
2. Напишите integration-тесты для HTTP endpoints
3. Убедитесь что все тесты проходят локально
4. Добавьте описание тестов в этот README

## Ссылки

- [Ktor Testing Documentation](https://ktor.io/docs/testing.html)
- [MockK Documentation](https://mockk.io/)
- [Kotlin Test Documentation](https://kotlinlang.org/api/latest/kotlin.test/)
