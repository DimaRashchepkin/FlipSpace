/**
 * Flashcard Study System with Spaced Repetition
 * Система изучения флип-карточек с интервальным повторением
 */

class FlashcardStudy {
    constructor(cards) {
        this.originalCards = cards;
        this.isAnswerShown = false;
        this.totalReviewed = 0;

        // Инициализируем карточки с рейтингом 1 (плохо знаем)
        this.cardPool = cards.map(card => ({
            ...card,
            rating: 1,
            reviewCount: 0,
            lastReviewed: 0
        }));

        this.initElements();
        this.bindEvents();
        this.selectNextCard();
    }

    initElements() {
        // Элементы карточки
        this.questionText = document.getElementById('question-text');
        this.answerText = document.getElementById('answer-text');
        this.cardNumber = document.getElementById('card-number');
        this.answerContainer = document.getElementById('answer-container');
        this.cardContainer = document.getElementById('card-container');

        // Кнопки
        this.showAnswerBtn = document.getElementById('show-answer-btn');
        this.showAnswerContainer = document.getElementById('show-answer-btn-container');
        this.ratingContainer = document.getElementById('rating-buttons-container');
        this.ratingButtons = document.querySelectorAll('.btn-rating');

        // Прогресс
        this.progressContainer = document.getElementById('progress-container');
        this.progressText = document.getElementById('progress-text');
    }

    bindEvents() {
        // Показать ответ
        this.showAnswerBtn.addEventListener('click', () => this.showAnswer());

        // Кнопки оценки
        this.ratingButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const rating = parseInt(e.currentTarget.dataset.rating);
                this.rateCard(rating);
            });
        });

        // Клавиатурные сокращения
        document.addEventListener('keydown', (e) => this.handleKeyPress(e));
    }

    /**
     * Выбор следующей карточки на основе весов
     * Карточки с низким рейтингом имеют больший вес
     */
    selectNextCard() {
        if (this.cardPool.length === 0) {
            this.showEmptyMessage();
            return;
        }

        // Вычисляем веса для каждой карточки
        const weights = this.cardPool.map(card => {
            // Базовый вес зависит от рейтинга (чем ниже рейтинг, тем выше вес)
            const ratingWeight = Math.pow(6 - card.rating, 2); // 1->25, 2->16, 3->9, 4->4, 5->1

            // Уменьшаем вес недавно просмотренных карточек
            const recencyPenalty = card.lastReviewed === this.totalReviewed ? 0.3 : 1;

            return ratingWeight * recencyPenalty;
        });

        // Выбираем карточку на основе весов (weighted random selection)
        const totalWeight = weights.reduce((sum, w) => sum + w, 0);
        let random = Math.random() * totalWeight;

        let selectedIndex = 0;
        for (let i = 0; i < weights.length; i++) {
            random -= weights[i];
            if (random <= 0) {
                selectedIndex = i;
                break;
            }
        }

        this.currentCard = this.cardPool[selectedIndex];
        this.currentCardIndex = selectedIndex;
        this.loadCard();
    }

    loadCard() {
        // Анимация появления новой карточки
        this.cardContainer.style.opacity = '0';
        this.cardContainer.style.transform = 'scale(0.95)';

        setTimeout(() => {
            // Обновляем содержимое
            this.questionText.textContent = this.currentCard.frontText;
            this.answerText.textContent = this.currentCard.backText;

            // Скрываем ответ
            this.answerContainer.style.display = 'none';
            this.showAnswerContainer.style.display = 'block';
            this.ratingContainer.style.display = 'none';
            this.isAnswerShown = false;

            // Анимация появления
            this.cardContainer.style.opacity = '1';
            this.cardContainer.style.transform = 'scale(1)';

            // Обновляем прогресс
            this.updateProgress();
        }, 200);
    }

    showAnswer() {
        if (this.isAnswerShown) return;

        this.isAnswerShown = true;

        // Показываем ответ с анимацией
        this.answerContainer.style.display = 'block';
        this.answerContainer.style.opacity = '0';
        this.answerContainer.style.transform = 'translateY(-10px)';

        setTimeout(() => {
            this.answerContainer.style.transition = 'all 0.3s ease';
            this.answerContainer.style.opacity = '1';
            this.answerContainer.style.transform = 'translateY(0)';
        }, 10);

        // Скрываем кнопку "Показать ответ"
        this.showAnswerContainer.style.display = 'none';

        // Показываем кнопки оценки с задержкой
        setTimeout(() => {
            this.ratingContainer.style.display = 'block';
            this.ratingContainer.style.opacity = '0';

            setTimeout(() => {
                this.ratingContainer.style.transition = 'opacity 0.3s ease';
                this.ratingContainer.style.opacity = '1';
            }, 10);
        }, 200);
    }

    rateCard(rating) {
        if (!this.isAnswerShown) return;

        // Обновляем рейтинг карточки (скользящее среднее)
        const card = this.currentCard;
        card.rating = (card.rating * card.reviewCount + rating) / (card.reviewCount + 1);
        card.reviewCount++;
        card.lastReviewed = this.totalReviewed;
        this.totalReviewed++;

        // Если карточка хорошо изучена (рейтинг >= 4.5), можем удалить из пула
        // Но оставим для бесконечного повторения, просто снизим вес

        // Анимация нажатия кнопки
        const button = document.querySelector(`[data-rating="${rating}"]`);
        button.style.transform = 'scale(0.95)';

        setTimeout(() => {
            button.style.transform = 'scale(1)';

            // Выбираем следующую карточку
            this.selectNextCard();
        }, 150);
    }

    updateProgress() {
        // Обновляем статистику в прогресс-баре
        const wellKnownCount = this.cardPool.filter(c => c.rating >= 4).length;
        const totalCards = this.cardPool.length;
        const progress = totalCards > 0 ? (wellKnownCount / totalCards) * 100 : 0;

        const progressBar = document.getElementById('progress-bar');
        const progressText = document.getElementById('progress-text');

        if (progressBar && progressText) {
            progressBar.style.width = `${progress}%`;
            progressBar.setAttribute('aria-valuenow', progress);
            progressText.textContent = `Хорошо изучено: ${wellKnownCount} / ${totalCards}`;
        }
    }

    getAverageRating() {
        if (this.cardPool.length === 0) return '0.0';
        const sum = this.cardPool.reduce((acc, card) => acc + card.rating, 0);
        return (sum / this.cardPool.length).toFixed(1);
    }

    showEmptyMessage() {
        document.getElementById('card-container').innerHTML = `
            <div class="glass-effect p-5 rounded-4 text-center">
                <i class="bi bi-inbox text-light opacity-50" style="font-size: 3rem;"></i>
                <h3 class="text-white mt-3">В этом наборе пока нет карточек</h3>
                <p class="text-light opacity-75">Добавьте карточки, чтобы начать изучение</p>
                <a href="/sets" class="btn btn-primary btn-lg mt-3">
                    <i class="bi bi-arrow-left me-2"></i>К наборам
                </a>
            </div>
        `;
        this.showAnswerContainer.style.display = 'none';
    }

    handleKeyPress(e) {
        // Игнорируем нажатия в полях ввода
        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
            return;
        }

        // Пробел - показать ответ
        if (e.code === 'Space' && !this.isAnswerShown) {
            e.preventDefault();
            this.showAnswer();
            return;
        }

        // Цифры 1-5 - оценка карточки
        if (this.isAnswerShown && e.key >= '1' && e.key <= '5') {
            e.preventDefault();
            const rating = parseInt(e.key);
            this.rateCard(rating);
        }
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    if (window.flashcards && window.flashcards.length > 0) {
        new FlashcardStudy(window.flashcards);
    } else {
        // Если карточек нет, показываем сообщение
        document.getElementById('card-container').innerHTML = `
            <div class="glass-effect p-5 rounded-4 text-center">
                <i class="bi bi-inbox text-light opacity-50" style="font-size: 3rem;"></i>
                <h3 class="text-white mt-3">В этом наборе пока нет карточек</h3>
                <p class="text-light opacity-75">Добавьте карточки, чтобы начать изучение</p>
                <a href="/sets" class="btn btn-primary btn-lg mt-3">
                    <i class="bi bi-arrow-left me-2"></i>К наборам
                </a>
            </div>
        `;
        document.getElementById('show-answer-btn-container').style.display = 'none';
    }
});
