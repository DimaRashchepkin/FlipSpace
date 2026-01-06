class CardStudySession {
    constructor() {
        this.currentSetId = this.getSetIdFromUrl();
        this.cards = [];
        this.currentCardIndex = 0;
        this.reviewedCards = 0;
        this.totalRatings = 0;
        this.ratingSum = 0;

        this.initElements();
        this.loadSetData();
        this.setupEventListeners();
    }

    getSetIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('setId') || 'demo-set';
    }

    initElements() {
        this.setTitleElement = document.getElementById('set-title');
        this.cardTitleElement = document.getElementById('card-title');
        this.questionElement = document.getElementById('question-text');
        this.answerElement = document.getElementById('answer-text');
        this.answerSection = document.getElementById('answer-section');
        this.showAnswerBtn = document.getElementById('show-answer-btn');
        this.ratingButtons = document.getElementById('rating-buttons');
        this.progressText = document.getElementById('progress-text');
        this.progressBar = document.getElementById('progress-bar');
        this.progressPercent = document.getElementById('progress-percent');
        this.cardsLeftElement = document.getElementById('cards-left');
        this.cardsReviewedElement = document.getElementById('cards-reviewed');
        this.averageRatingElement = document.getElementById('average-rating');
        this.exitStudyBtn = document.getElementById('exit-study-btn');
    }

    setupEventListeners() {
        this.showAnswerBtn.addEventListener('click', () => this.showAnswer());

        document.querySelectorAll('.rating-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const rating = parseInt(e.target.dataset.rating);
                this.rateCard(rating);
            });
        });

        this.exitStudyBtn.addEventListener('click', () => {
            if (confirm('Завершить изучение набора?')) {
                window.location.href = '/sets';
            }
        });
    }

    loadSetData() {
        const mockCards = [
            {
                id: 1,
                title: 'Основы программирования',
                question: 'Что такое переменная в программировании?',
                answer: 'Переменная — это именованная область памяти, которая хранит данные, которые могут изменяться в процессе выполнения программы.'
            },
            {
                id: 2,
                title: 'Структуры данных',
                question: 'Чем отличается массив от связного списка?',
                answer: 'Массив имеет фиксированный размер и непрерывное хранение в памяти, тогда как связный список состоит из узлов с указателями и может динамически изменять размер.'
            },
            {
                id: 3,
                title: 'Алгоритмы',
                question: 'Что такое Big O нотация?',
                answer: 'Big O нотация описывает асимптотическую сложность алгоритма в худшем случае, показывая, как растет время выполнения или использование памяти при увеличении объема входных данных.'
            },
            {
                id: 4,
                title: 'Базы данных',
                question: 'Что такое нормализация баз данных?',
                answer: 'Нормализация — процесс организации данных в базе для уменьшения избыточности и улучшения целостности данных путем разделения таблиц и установления отношений между ними.'
            },
            {
                id: 5,
                title: 'ООП',
                question: 'Назовите три основных принципа ООП',
                answer: '1. Инкапсуляция — скрытие внутренней реализации. 2. Наследование — создание новых классов на основе существующих. 3. Полиморфизм — возможность объектов с одинаковым интерфейсом иметь разную реализацию.'
            }
        ];

        this.cards = [...mockCards];
        this.setTitleElement.textContent = 'Программирование для начинающих';
        this.displayCurrentCard();
        this.updateProgress();
        this.updateStatistics();
    }

    displayCurrentCard() {
        if (this.currentCardIndex >= this.cards.length) {
            this.showCompletionScreen();
            return;
        }

        const card = this.cards[this.currentCardIndex];
        this.cardTitleElement.textContent = `Карточка #${this.currentCardIndex + 1}`;
        this.questionElement.textContent = card.question;
        this.answerElement.textContent = card.answer;

        this.answerSection.style.display = 'none';
        this.showAnswerBtn.style.display = 'block';
        this.ratingButtons.style.display = 'none';
    }

    showAnswer() {
        this.answerSection.style.display = 'block';
        this.showAnswerBtn.style.display = 'none';
        this.ratingButtons.style.display = 'block';
    }

    rateCard(rating) {
        this.reviewedCards++;
        this.totalRatings++;
        this.ratingSum += rating;

        const currentCard = this.cards[this.currentCardIndex];
        console.log(`Оценка карточки "${currentCard.title}": ${rating}/5`);

        this.currentCardIndex++;
        this.updateProgress();
        this.updateStatistics();

        if (this.currentCardIndex < this.cards.length) {
            this.displayCurrentCard();
        } else {
            this.showCompletionScreen();
        }
    }

    updateProgress() {
        const total = this.cards.length;
        const completed = this.currentCardIndex;
        const progress = total > 0 ? Math.round((completed / total) * 100) : 0;

        this.progressText.textContent = `${completed}/${total}`;
        this.progressBar.style.width = `${progress}%`;
        this.progressPercent.textContent = `${progress}%`;
    }

    updateStatistics() {
        const total = this.cards.length;
        const left = total - this.currentCardIndex;
        const avgRating = this.totalRatings > 0 ? (this.ratingSum / this.totalRatings).toFixed(1) : '0.0';

        this.cardsLeftElement.textContent = left;
        this.cardsReviewedElement.textContent = this.reviewedCards;
        this.averageRatingElement.textContent = avgRating;
    }

    showCompletionScreen() {
        this.cardTitleElement.textContent = 'Набор пройден!';
        this.questionElement.textContent = 'Поздравляем! Вы успешно изучили все карточки в этом наборе.';
        this.answerSection.style.display = 'none';
        this.showAnswerBtn.style.display = 'none';
        this.ratingButtons.style.display = 'none';

        const avgRating = this.totalRatings > 0 ? (this.ratingSum / this.totalRatings).toFixed(1) : '0.0';
        this.questionElement.innerHTML += `<br><br><div class="text-center"><span class="badge bg-success fs-6">Средняя оценка: ${avgRating}/5</span></div>`;

        this.updateProgress();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new CardStudySession();

    console.log('FlipSpace Study Session initialized');
    console.log('Test commands:');
    console.log('- Type "study.showAnswer()" to show current answer');
    console.log('- Type "study.rateCard(3)" to rate current card (1-5)');
    console.log('- Type "study.nextCard()" to move to next card');
});