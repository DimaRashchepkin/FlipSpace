# FlipSpace

Веб-приложение для изучения материала с помощью карточек (flashcards), разработанное на Ktor.

## Описание проекта

FlipSpace - это веб-приложение для создания и изучения учебных карточек. Пользователи могут:
- Регистрироваться и авторизоваться в системе
- Создавать наборы карточек (card sets)
- Добавлять карточки с вопросами и ответами
- Изучать материал в режиме тренировки
- Управлять приватностью своих наборов карточек

## Технологический стек

- **Backend**: Ktor 3.3.1 (Kotlin)
- **Database**: PostgreSQL 15
- **Template Engine**: Pebble 3.2.2
- **Build Tool**: Gradle
- **Authentication**: Session-based с bcrypt
- **Testing**: JUnit, MockK

## Требования к системе

### Обязательные требования

1. **Java Development Kit (JDK) 17**
   - Проект использует Kotlin с JVM таргетом версии 17
   - Скачать JDK 17 можно:
     - [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
     - [OpenJDK 17](https://adoptium.net/temurin/releases/?version=17)
     - [Amazon Corretto 17](https://aws.amazon.com/corretto/)

2. **Docker и Docker Compose**
   - Для запуска базы данных PostgreSQL
   - Скачать: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

3. **Git** (для клонирования репозитория)

### Проверка установленных версий

```bash
# Проверка версии Java
java -version
# Должно быть: openjdk version "17.x.x" или java version "17.x.x"

# Проверка версии Docker
docker --version
docker-compose --version
```

## Инструкция по запуску

### Шаг 1: Клонирование репозитория

```bash
git clone <repository-url>
cd FlipSpace
```

### Шаг 2: Запуск базы данных PostgreSQL

Проект использует Docker Compose для управления базой данных. База данных будет автоматически инициализирована с помощью файла `init.sql`.

**Запуск контейнера с базой данных:**

```bash
docker-compose up -d
```

Эта команда:
- Скачает образ PostgreSQL 15 (если его нет локально)
- Создаст контейнер `flipspace_db`
- Запустит PostgreSQL на порту 5432
- Автоматически выполнит скрипт `init.sql` для инициализации схемы базы данных
- Создаст volume `postgres_data` для сохранения данных

**Параметры подключения к базе данных:**
- Host: `localhost`
- Port: `5432`
- Database: `flipspace`
- Username: `flipspace_user`
- Password: `flipspace_password`

**Проверка запуска базы данных:**

```bash
# Проверить статус контейнера
docker ps

# Должен быть контейнер flipspace_db в состоянии Up
```

**Остановка базы данных:**

```bash
docker-compose down
```

**Полная очистка (удаление данных):**

```bash
docker-compose down -v
```

### Шаг 3: Сброс базы данных (опционально)

Если нужно пересоздать базу данных с начальными данными:

**Для Windows:**
```powershell
.\reset-db.ps1
```

**Для Linux/Mac:**
```bash
PGPASSWORD=flipspace_password psql -h localhost -U flipspace_user -d flipspace -f init.sql
```

### Шаг 4: Запуск приложения

**Вариант 1: Запуск через Gradle (рекомендуется для разработки)**

```bash
./gradlew run
```

**Вариант 2: Сборка и запуск JAR-файла**

```bash
# Сборка fat JAR со всеми зависимостями
./gradlew buildFatJar

# Запуск JAR-файла
java -jar build/libs/flipspace-0.0.1-all.jar
```

**Вариант 3: Запуск через Docker**

```bash
# Сборка Docker-образа
./gradlew buildImage

# Запуск приложения в Docker
./gradlew runDocker
```

### Шаг 5: Проверка работы приложения

После успешного запуска вы увидите в консоли:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

**Доступные URL:**
- Главная страница: http://localhost:8080/
- Health Check: http://localhost:8080/health
- Вход: http://localhost:8080/login
- Регистрация: http://localhost:8080/register

## Структура базы данных

База данных состоит из трех основных таблиц:

1. **users** - Пользователи системы
   - id, login, password (bcrypt hash), created_at

2. **card_sets** - Наборы карточек
   - id, user_id, title, is_private, created_at, updated_at

3. **cards** - Карточки с вопросами и ответами
   - id, set_id, title, front_text, back_text, created_at, updated_at

## Команды для разработки

### Тестирование

```bash
# Запуск всех тестов
./gradlew test

# Запуск тестов с отчетом
./gradlew test --info
```

### Сборка проекта

```bash
# Полная сборка проекта
./gradlew build

# Сборка fat JAR (исполняемый файл со всеми зависимостями)
./gradlew buildFatJar

# Сборка Docker-образа
./gradlew buildImage

# Публикация образа в локальный registry
./gradlew publishImageToLocalRegistry
```

### Проверка качества кода

```bash
# Проверка стиля кода (ktlint + detekt)
./gradlew check

# Автоматическое исправление стиля кода
./gradlew fixCodeStyle

# Только ktlint проверка
./gradlew ktlintCheck

# Только detekt анализ
./gradlew detekt
```

### Очистка проекта

```bash
# Удаление build директории
./gradlew clean

# Полная пересборка
./gradlew clean build
```

## Работа с градлом в Windows

Для Windows используйте `gradlew.bat` вместо `./gradlew`:

```cmd
gradlew.bat run
gradlew.bat test
gradlew.bat build
```

## Решение проблем

### База данных не запускается

```bash
# Проверить логи контейнера
docker logs flipspace_db

# Перезапустить контейнер
docker-compose restart

# Полная переустановка
docker-compose down -v
docker-compose up -d
```

### Порт 8080 уже занят

Измените порт в файле [src/main/resources/application.yaml](src/main/resources/application.yaml):

```yaml
ktor:
    deployment:
        port: 8081  # Измените на свободный порт
```

### Порт 5432 (PostgreSQL) уже занят

Если на вашей системе уже запущен PostgreSQL, измените порт в [docker-compose.yml](docker-compose.yml):

```yaml
ports:
  - "5433:5432"  # Внешний порт 5433, внутренний остается 5432
```

Также нужно будет обновить подключение к БД в коде.

### Ошибки компиляции Kotlin

Убедитесь, что используете JDK 17:

```bash
# Установить JAVA_HOME
export JAVA_HOME=/path/to/jdk-17  # Linux/Mac
set JAVA_HOME=C:\path\to\jdk-17   # Windows

# Проверить
echo $JAVA_HOME  # Linux/Mac
echo %JAVA_HOME%  # Windows
```

## Технологии и библиотеки

### Основной стек

| Технология | Версия | Описание |
|-----------|--------|----------|
| Kotlin | 2.2.20 | Язык программирования |
| Ktor | 3.3.1 | Web framework |
| PostgreSQL | 15 | База данных |
| Gradle | 8.x | Система сборки |

### Ktor плагины

| Плагин | Описание |
|--------|----------|
| [Routing](https://ktor.io/docs/routing-in-ktor.html) | Маршрутизация запросов |
| [Content Negotiation](https://ktor.io/docs/serialization.html) | Автоматическая сериализация/десериализация |
| [kotlinx.serialization](https://ktor.io/docs/kotlin-serialization.html) | JSON сериализация |
| [Sessions](https://ktor.io/docs/sessions.html) | Управление сессиями пользователей |
| [Pebble](https://ktor.io/docs/pebble.html) | Template engine для HTML страниц |
| [Static Content](https://ktor.io/docs/static-content.html) | Раздача статических файлов (CSS, JS, изображения) |
| [Call Logging](https://ktor.io/docs/call-logging.html) | Логирование запросов |
| [Request Validation](https://ktor.io/docs/request-validation.html) | Валидация входящих данных |

### Дополнительные библиотеки

- **bcrypt** (0.10.2) - Хеширование паролей
- **Logback** (1.4.14) - Логирование
- **PostgreSQL JDBC** (42.7.8) - Драйвер для PostgreSQL
- **H2 Database** (2.3.232) - Embedded БД для тестов
- **MockK** (1.13.8) - Библиотека для моков в тестах

## Архитектура проекта

```
src/
├── main/
│   ├── kotlin/ru/yarsu/
│   │   ├── Application.kt          # Точка входа
│   │   ├── db/                     # Работа с базой данных
│   │   │   ├── DatabaseFactory.kt
│   │   │   ├── UserDatabaseService.kt
│   │   │   ├── CardSetDatabaseService.kt
│   │   │   └── CardDatabaseService.kt
│   │   ├── models/                 # Модели данных
│   │   │   ├── User.kt
│   │   │   ├── CardSet.kt
│   │   │   └── Card.kt
│   │   ├── services/               # Бизнес-логика
│   │   │   ├── AuthService.kt
│   │   │   └── CardSetService.kt
│   │   └── web/                    # Веб-слой
│   │       ├── controllers/        # Контроллеры
│   │       ├── routes/             # Определение маршрутов
│   │       └── Routing.kt
│   └── resources/
│       ├── application.yaml        # Конфигурация
│       ├── static/                 # CSS, JS
│       └── templates/              # HTML шаблоны (Pebble)
└── test/                           # Тесты
```

## Полезные ссылки

- [Документация Ktor](https://ktor.io/docs/home.html)
- [Ktor GitHub](https://github.com/ktorio/ktor)
- [Pebble Template Engine](https://pebbletemplates.io/)
- [PostgreSQL документация](https://www.postgresql.org/docs/)
- [Kotlin документация](https://kotlinlang.org/docs/home.html)

## Лицензия

Этот проект создан с использованием [Ktor Project Generator](https://start.ktor.io).

