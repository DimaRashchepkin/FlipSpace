-- Создание таблицы пользователей (остается без изменений)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы наборов карточек (НОВАЯ таблица)
CREATE TABLE IF NOT EXISTS bundles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы карточек (ОБНОВЛЕНА - добавлено поле bundle_id)
CREATE TABLE IF NOT EXISTS cards (
    id SERIAL PRIMARY KEY,
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bundle_id INTEGER REFERENCES bundles(id) ON DELETE SET NULL, -- НОВОЕ поле
    content TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1 CHECK (priority BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_cards_author_id ON cards(author_id);
CREATE INDEX IF NOT EXISTS idx_cards_priority ON cards(priority);
CREATE INDEX IF NOT EXISTS idx_cards_bundle_id ON cards(bundle_id); -- НОВЫЙ индекс
CREATE INDEX IF NOT EXISTS idx_bundles_author_id ON bundles(author_id);

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры для автоматического обновления updated_at
CREATE OR REPLACE TRIGGER update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER update_bundles_updated_at
    BEFORE UPDATE ON bundles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Вставка тестовых данных
INSERT INTO users (login, password) VALUES
('test_user', 'test_password'),
('admin', 'admin123');

-- Вставка тестовых наборов карточек
INSERT INTO bundles (name, description, author_id, is_public) VALUES
('Основные фразы', 'Базовые фразы для изучения языка', 1, TRUE),
('Сложные слова', 'Слова с высокой сложностью запоминания', 1, FALSE),
('Админский набор', 'Набор для администраторов', 2, TRUE);

-- Вставка тестовых карточек с привязкой к наборам
INSERT INTO cards (author_id, bundle_id, content, priority) VALUES
(1, 1, 'Hello - Привет', 1),
(1, 1, 'Goodbye - До свидания', 2),
(1, 2, 'Ephemeral - Мимолетный', 3),
(1, 2, 'Quintessential - Квинтэссенция', 4),
(2, 3, 'Административная карточка', 1);