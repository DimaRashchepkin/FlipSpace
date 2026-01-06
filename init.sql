DROP TABLE IF EXISTS card_sets CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE card_sets (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
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
    id VARCHAR(36) PRIMARY KEY,
    author_id VARCHAR(36) NOT NULL,
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
('00000000-0000-0000-0000-000000000001', 'test_user', 'test_password'),
('00000000-0000-0000-0000-000000000002', 'admin', 'admin123'),
('00000000-0000-0000-0000-000000000003', 'user_1', 'password123'),
('00000000-0000-0000-0000-000000000004', 'user_2', 'password456'),
('00000000-0000-0000-0000-000000000005', 'user_3', 'password789');

INSERT INTO card_sets (id, user_id, title, description) VALUES
('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000003', 'Программирование', 'Основы программирования'),
('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000003', 'Английский язык', 'Слова и фразы'),
('33333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000000003', 'Математика', 'Формулы и теоремы'),
('44444444-4444-4444-4444-444444444444', '00000000-0000-0000-0000-000000000004', 'Философия', 'Основные философские течения'),
('55555555-5555-5555-5555-555555555555', '00000000-0000-0000-0000-000000000005', 'Экономика', 'Микро и макроэкономика');

INSERT INTO cards (id, author_id, content, priority) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '00000000-0000-0000-0000-000000000003', 'Карточка пользователя 3', 4),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '00000000-0000-0000-0000-000000000003', 'Еще одна карточка', 1),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '00000000-0000-0000-0000-000000000004', 'Карточка пользователя 4', 2);