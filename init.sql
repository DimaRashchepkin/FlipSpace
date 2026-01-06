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
    is_private BOOLEAN DEFAULT FALSE,
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
);

CREATE INDEX idx_cards_set_id ON cards(set_id);
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

INSERT INTO card_sets (id, user_id, title) VALUES
('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000003', 'Программирование'),
('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000003', 'Английский язык'),
('33333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000000003', 'Математика'),
('44444444-4444-4444-4444-444444444444', '00000000-0000-0000-0000-000000000004', 'Философия'),
('55555555-5555-5555-5555-555555555555', '00000000-0000-0000-0000-000000000005', 'Экономика');

INSERT INTO cards (id, set_id, front_text, back_text) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Что такое переменная?', 'Переменная - это именованная область памяти для хранения данных'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'Что такое функция?', 'Функция - это блок кода, который выполняет определенную задачу'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'Hello', 'Привет'),
('dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'Goodbye', 'До свидания');