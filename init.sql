-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы карточек
CREATE TABLE IF NOT EXISTS cards (
    id SERIAL PRIMARY KEY,
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1 CHECK (priority BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска карточек по автору
CREATE INDEX IF NOT EXISTS idx_cards_author_id ON cards(author_id);

-- Индекс для приоритета
CREATE INDEX IF NOT EXISTS idx_cards_priority ON cards(priority);

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггер для автоматического обновления updated_at
CREATE OR REPLACE TRIGGER update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Вставка тестовых данных
INSERT INTO users (login, password) VALUES
('test_user', 'test_password'),
('admin', 'admin123');

INSERT INTO cards (author_id, content, priority) VALUES
(1, 'Первая карточка для изучения', 1),
(1, 'Вторая карточка с более высоким приоритетом', 3),
(2, 'Админская карточка', 2);