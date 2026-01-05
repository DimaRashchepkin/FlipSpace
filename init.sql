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

-- Создание таблицы наборов карточек (card_sets)
CREATE TABLE IF NOT EXISTS card_sets (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_cards_author_id ON cards(author_id);
CREATE INDEX IF NOT EXISTS idx_cards_priority ON cards(priority);
CREATE INDEX IF NOT EXISTS idx_card_sets_user_id ON card_sets(user_id);

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры
CREATE OR REPLACE TRIGGER update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE TRIGGER update_card_sets_updated_at
    BEFORE UPDATE ON card_sets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Тестовые данные пользователей
INSERT INTO users (login, password) VALUES
('test_user', 'test_password'),
('admin', 'admin123'),
('user_12345', 'password123')
ON CONFLICT (login) DO NOTHING;

-- Тестовые данные карточек
INSERT INTO cards (author_id, content, priority) VALUES
(1, 'Первая карточка для изучения', 1),
(1, 'Вторая карточка с более высоким приоритетом', 3),
(2, 'Админская карточка', 2),
(3, 'Карточка пользователя 12345', 4),
(3, 'Еще одна карточка для изучения', 1)
ON CONFLICT DO NOTHING;

-- Тестовые данные наборов карточек
INSERT INTO card_sets (id, user_id, title, description) VALUES
-- Наборы пользователя '12345' (10 наборов)
('11111111-1111-1111-1111-111111111111', '12345', 'Программирование', 'Основы программирования и алгоритмы'),
('22222222-2222-2222-2222-222222222222', '12345', 'Английский язык', 'Слова и фразы для начинающих'),
('33333333-3333-3333-3333-333333333333', '12345', 'Математика', 'Формулы и теоремы'),
('44444444-4444-4444-4444-444444444444', '12345', 'История', 'Важные исторические события'),
('55555555-5555-5555-5555-555555555555', '12345', 'Биология', 'Строение клетки и органеллы'),
('66666666-6666-6666-6666-666666666666', '12345', 'Физика', 'Законы Ньютона и механика'),
('77777777-7777-7777-7777-777777777777', '12345', 'Химия', 'Периодическая таблица элементов'),
('88888888-8888-8888-8888-888888888888', '12345', 'География', 'Столицы и флаги стран'),
('99999999-9999-9999-9999-999999999999', '12345', 'Литература', 'Классические произведения'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '12345', 'Искусство', 'Стили и направления в искусстве'),

-- Наборы других пользователей (10 наборов)
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'user_2', 'Философия', 'Основные философские течения'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'user_2', 'Экономика', 'Микро и макроэкономика'),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'user_3', 'Психология', 'Теории личности и поведения'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'user_3', 'Социология', 'Социальные структуры и процессы'),
('ffffffff-ffff-ffff-ffff-ffffffffffff', 'user_4', 'Право', 'Основы правовых систем'),
('gggggggg-gggg-gggg-gggg-gggggggggggg', 'user_4', 'Маркетинг', 'Стратегии продвижения'),
('hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh', 'user_5', 'Дизайн', 'Принципы композиции и цвета'),
('iiiiiiii-iiii-iiii-iiii-iiiiiiiiiiii', 'user_5', 'Музыка', 'Нотная грамота и теория'),
('jjjjjjjj-jjjj-jjjj-jjjj-jjjjjjjjjjjj', 'user_6', 'Спорт', 'Правила и техники'),
('kkkkkkkk-kkkk-kkkk-kkkk-kkkkkkkkkkkk', 'user_6', 'Кулинария', 'Рецепты и техники приготовления')
ON CONFLICT (id) DO NOTHING;