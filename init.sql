DROP TABLE IF EXISTS card_sets CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE card_sets (
    id VARCHAR(36) PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_sets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE cards (
    id SERIAL PRIMARY KEY,
    author_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1 CHECK (priority BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE INDEX idx_cards_author_id ON cards(author_id);
CREATE INDEX idx_cards_priority ON cards(priority);
CREATE INDEX idx_card_sets_user_id ON card_sets(user_id);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE TRIGGER update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER update_card_sets_updated_at
    BEFORE UPDATE ON card_sets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

INSERT INTO users (id, login, password) VALUES
(1, 'test_user', 'test_password'),
(2, 'admin', 'admin123'),
(3, 'user_1', 'password123'),
(4, 'user_2', 'password456'),
(5, 'user_3', 'password789');

INSERT INTO card_sets (id, user_id, title, description) VALUES
('11111111-1111-1111-1111-111111111111', 3, 'Программирование', 'Основы программирования'),
('22222222-2222-2222-2222-222222222222', 3, 'Английский язык', 'Слова и фразы'),
('33333333-3333-3333-3333-333333333333', 3, 'Математика', 'Формулы и теоремы'),
('44444444-4444-4444-4444-444444444444', 4, 'Философия', 'Основные философские течения'),
('55555555-5555-5555-5555-555555555555', 5, 'Экономика', 'Микро и макроэкономика');

INSERT INTO cards (author_id, content, priority) VALUES
(3, 'Карточка пользователя 3', 4),
(3, 'Еще одна карточка', 1),
(4, 'Карточка пользователя 4', 2);