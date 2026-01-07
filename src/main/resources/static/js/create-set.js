// Файл: static/js/create-set.js
// Скрипт для страницы создания набора карточек

document.addEventListener('DOMContentLoaded', function() {
    const addCardBtn = document.getElementById('add-card-btn');
    const cardTitleInput = document.getElementById('cardTitle');
    const cardQuestionInput = document.getElementById('cardQuestion');
    const cardAnswerInput = document.getElementById('cardAnswer');
    const cardsList = document.getElementById('cards-list');
    const cardsCount = document.getElementById('cards-count');
    const cardsDataContainer = document.getElementById('cards-data-container');
    const form = document.getElementById('create-set-form');

    // Получаем начальный индекс из data-атрибута или из количества существующих карточек
    const existingCards = document.querySelectorAll('#cards-list .list-group-item:not(.text-center)');
    let cardIndex = existingCards.length;

    // Переменные для редактирования карточки
    let editingCardIndex = null;
    let editingCardElement = null;

    // Функция для обновления счетчика карточек
    function updateCardsCount() {
        const cards = document.querySelectorAll('#cards-list .list-group-item');
        cardsCount.textContent = cards.length;

        // Показываем/скрываем placeholder
        const placeholder = cardsList.querySelector('.text-center');
        if (cards.length === 0 && !placeholder) {
            cardsList.innerHTML = `
                <div class="text-center py-5">
                    <i class="bi bi-card-text display-4 text-white mb-3"></i>
                    <p class="text-light opacity-75">Карточки пока не добавлены</p>
                </div>
            `;
        } else if (cards.length > 0 && placeholder) {
            cardsList.innerHTML = '';
        }
    }

    // Функция добавления карточки
    function addCardToSet(title, question, answer) {
        // Проверяем, есть ли placeholder
        if (cardsList.children.length > 0 && cardsList.children[0].classList.contains('text-center')) {
            cardsList.innerHTML = '';
        }

        // Определяем отображаемое название - используем только title, если его нет - "Карточка N"
        const displayTitle = title || `Карточка ${cardIndex + 1}`;

        // Создаем элемент списка
        const cardElement = document.createElement('div');
        cardElement.className = 'list-group-item glass-effect border-glass mb-2 rounded-3 d-flex justify-content-between align-items-center';
        cardElement.style.cursor = 'pointer';
        cardElement.innerHTML = `
            <span class="text-truncate text-white">${displayTitle}</span>
            <button type="button" class="btn btn-sm btn-outline-light border-0 remove-card-btn" data-card-index="${cardIndex}">
                <i class="bi bi-x-lg"></i>
            </button>
        `;

        // Добавляем в начало списка
        cardsList.prepend(cardElement);

        // Создаем скрытые поля для формы
        const hiddenInputs = document.createElement('div');
        hiddenInputs.innerHTML = `
            <input type="hidden" name="cards[${cardIndex}].title" value="${escapeHtml(title || '')}">
            <input type="hidden" name="cards[${cardIndex}].question" value="${escapeHtml(question || '')}">
            <input type="hidden" name="cards[${cardIndex}].answer" value="${escapeHtml(answer || '')}">
        `;
        cardsDataContainer.appendChild(hiddenInputs);

        // Увеличиваем индекс
        cardIndex++;

        // Обновляем счетчик
        updateCardsCount();
    }

    // Функция экранирования HTML
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Обработчик клика на кнопку добавления/редактирования
    addCardBtn.addEventListener('click', function() {
        const title = cardTitleInput.value.trim();
        const question = cardQuestionInput.value.trim();
        const answer = cardAnswerInput.value.trim();

        // Проверяем, что заполнены хотя бы вопрос или ответ
        if (!question && !answer) {
            showAlert('Пожалуйста, заполните хотя бы одно из полей: вопрос или ответ', 'warning');
            return;
        }

        // Если редактируем карточку
        if (editingCardIndex !== null) {
            updateCard(editingCardIndex, title, question, answer);
            cancelEditing();
        } else {
            // Добавляем новую карточку
            addCardToSet(title, question, answer);
        }

        // Очищаем поля
        cardTitleInput.value = '';
        cardQuestionInput.value = '';
        cardAnswerInput.value = '';

        // Фокусируемся на поле названия
        cardTitleInput.focus();
    });

    // Обработчик нажатия Enter в полях ввода
    [cardTitleInput, cardQuestionInput, cardAnswerInput].forEach(input => {
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                if (input === cardAnswerInput) {
                    addCardBtn.click();
                } else {
                    // Переходим к следующему полю
                    const nextInput = input.nextElementSibling;
                    if (nextInput && nextInput.tagName === 'TEXTAREA') {
                        nextInput.focus();
                    }
                }
            }
        });
    });

    // Обработчик кликов на список карточек (удаление и редактирование)
    cardsList.addEventListener('click', function(e) {
        // Удаление карточки
        if (e.target.closest('.remove-card-btn')) {
            const button = e.target.closest('.remove-card-btn');
            const cardIndexToRemove = button.dataset.cardIndex;

            // Если удаляем редактируемую карточку, отменяем редактирование
            if (editingCardIndex === parseInt(cardIndexToRemove)) {
                cancelEditing();
            }

            // Удаляем элемент из DOM
            const cardElement = button.closest('.list-group-item');
            cardElement.remove();

            // Удаляем соответствующие скрытые поля
            const hiddenInputs = cardsDataContainer.querySelectorAll(`input[name^="cards[${cardIndexToRemove}]"]`);
            hiddenInputs.forEach(input => input.remove());

            // Обновляем индексы оставшихся карточек
            updateCardIndexes();

            // Обновляем счетчик
            updateCardsCount();
        }
        // Редактирование карточки (клик на саму карточку, но не на кнопку удаления)
        else if (e.target.closest('.list-group-item') && !e.target.closest('.remove-card-btn')) {
            const cardElement = e.target.closest('.list-group-item');
            const cardIndexToEdit = parseInt(cardElement.querySelector('.remove-card-btn').dataset.cardIndex);

            // Загружаем данные карточки для редактирования
            loadCardForEditing(cardIndexToEdit, cardElement);
        }
    });

    // Функция обновления индексов карточек
    function updateCardIndexes() {
        const cardElements = cardsList.querySelectorAll('.list-group-item');
        const hiddenInputs = cardsDataContainer.querySelectorAll('input[name^="cards["]');

        // Создаем временный массив для данных карточек
        const cardsData = [];

        // Собираем данные из существующих скрытых полей
        hiddenInputs.forEach((input, index) => {
            const match = input.name.match(/cards\[(\d+)\]\.(\w+)/);
            if (match) {
                const oldIndex = parseInt(match[1]);
                const field = match[2];

                if (!cardsData[oldIndex]) {
                    cardsData[oldIndex] = {};
                }
                cardsData[oldIndex][field] = input.value;
            }
        });

        // Очищаем контейнер со скрытыми полями
        cardsDataContainer.innerHTML = '';

        // Пересоздаем скрытые поля с новыми индексами
        cardElements.forEach((cardElement, newIndex) => {
            const button = cardElement.querySelector('.remove-card-btn');
            const oldIndex = parseInt(button.dataset.cardIndex);
            button.dataset.cardIndex = newIndex;

            // Находим данные для этой карточки
            const cardData = cardsData[oldIndex] || {};

            // Создаем новые скрытые поля
            const hiddenInputs = document.createElement('div');
            hiddenInputs.innerHTML = `
                <input type="hidden" name="cards[${newIndex}].title" value="${escapeHtml(cardData.title || '')}">
                <input type="hidden" name="cards[${newIndex}].question" value="${escapeHtml(cardData.question || '')}">
                <input type="hidden" name="cards[${newIndex}].answer" value="${escapeHtml(cardData.answer || '')}">
            `;
            cardsDataContainer.appendChild(hiddenInputs);
        });

        // Обновляем cardIndex
        cardIndex = cardElements.length;
    }

    // Функция загрузки карточки для редактирования
    function loadCardForEditing(cardIndexToEdit, cardElement) {
        // Проверяем, что cardIndexToEdit является числом
        if (isNaN(cardIndexToEdit)) {
            return;
        }

        // Получаем данные карточки из скрытых полей
        const titleInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToEdit}].title"]`);
        const questionInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToEdit}].question"]`);
        const answerInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToEdit}].answer"]`);

        if (!titleInput || !questionInput || !answerInput) {
            return;
        }

        // Убираем выделение со старой карточки, если она есть
        if (editingCardElement && editingCardElement !== cardElement) {
            editingCardElement.style.backgroundColor = '';
            editingCardElement.style.border = '';
        }

        // Заполняем поля формы
        cardTitleInput.value = titleInput.value;
        cardQuestionInput.value = questionInput.value;
        cardAnswerInput.value = answerInput.value;

        // Устанавливаем режим редактирования
        editingCardIndex = cardIndexToEdit;
        editingCardElement = cardElement;

        // Обновляем UI
        updateEditingUI();

        // Фокусируемся на первом поле
        cardTitleInput.focus();
    }

    // Функция обновления карточки
    function updateCard(cardIndexToUpdate, title, question, answer) {
        // Обновляем скрытые поля
        const titleInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToUpdate}].title"]`);
        const questionInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToUpdate}].question"]`);
        const answerInput = cardsDataContainer.querySelector(`input[name="cards[${cardIndexToUpdate}].answer"]`);

        if (titleInput && questionInput && answerInput) {
            titleInput.value = title;
            questionInput.value = question;
            answerInput.value = answer;

            // Обновляем отображаемое название в списке (используем только title)
            const displayTitle = title || `Карточка ${cardIndexToUpdate + 1}`;
            const cardElement = editingCardElement;
            if (cardElement) {
                const titleSpan = cardElement.querySelector('span.text-truncate');
                if (titleSpan) {
                    titleSpan.textContent = displayTitle;
                }
            }

            showAlert('Карточка успешно обновлена', 'success');
        }
    }

    // Функция отмены редактирования
    function cancelEditing() {
        editingCardIndex = null;
        editingCardElement = null;

        // Очищаем поля
        cardTitleInput.value = '';
        cardQuestionInput.value = '';
        cardAnswerInput.value = '';

        // Обновляем UI
        updateEditingUI();
    }

    // Функция обновления UI в зависимости от режима редактирования
    function updateEditingUI() {
        const editingIndicator = document.getElementById('editing-indicator');
        const editingCardName = document.getElementById('editing-card-name');
        const cancelBtn = document.getElementById('cancel-editing-btn');

        if (editingCardIndex !== null) {
            // Режим редактирования
            addCardBtn.innerHTML = '<i class="bi bi-pencil-fill me-2"></i>Редактировать карточку';

            // Показываем индикатор редактирования
            if (editingIndicator) {
                editingIndicator.style.display = 'inline';
                if (editingCardName) {
                    // Используем title как название для отображения
                    const displayName = cardTitleInput.value || 'Без названия';
                    editingCardName.textContent = displayName;
                }
                if (cancelBtn) {
                    cancelBtn.style.display = 'inline';
                    // Добавляем обработчик только один раз
                    cancelBtn.onclick = cancelEditing;
                }
            }

            // Подсвечиваем редактируемую карточку
            if (editingCardElement) {
                editingCardElement.style.backgroundColor = 'rgba(13, 110, 253, 0.2)';
                editingCardElement.style.border = '2px solid #0d6efd';
            }
        } else {
            // Обычный режим
            addCardBtn.innerHTML = '<i class="bi bi-plus-circle me-2"></i>Добавить карточку в набор';

            // Скрываем индикатор редактирования
            if (editingIndicator) {
                editingIndicator.style.display = 'none';
            }
            if (cancelBtn) {
                cancelBtn.style.display = 'none';
            }

            // Убираем подсветку со всех карточек
            const allCards = cardsList.querySelectorAll('.list-group-item');
            allCards.forEach(card => {
                card.style.backgroundColor = '';
                card.style.border = '';
            });
        }
    }

    // Функция показа уведомления
    function showAlert(message, type = 'info') {
        // Создаем элемент уведомления
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show glass-effect border-glass text-white`;
        alertDiv.style.position = 'fixed';
        alertDiv.style.top = '20px';
        alertDiv.style.right = '20px';
        alertDiv.style.zIndex = '1000';
        alertDiv.style.minWidth = '300px';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="alert"></button>
        `;

        // Добавляем на страницу
        document.body.appendChild(alertDiv);

        // Удаляем через 3 секунды
        setTimeout(() => {
            alertDiv.remove();
        }, 3000);
    }

    // Обработчик отправки формы
    form.addEventListener('submit', function(e) {
        const cardsCountValue = parseInt(cardsCount.textContent);
        const setTitle = document.getElementById('setTitle').value.trim();

        if (!setTitle) {
            e.preventDefault();
            showAlert('Пожалуйста, введите название набора', 'warning');
            return;
        }

        if (cardsCountValue === 0) {
            e.preventDefault();
            showAlert('Пожалуйста, добавьте хотя бы одну карточку в набор', 'warning');
            return;
        }
    });

    // Инициализация при загрузке страницы
    updateCardsCount();

    // Инициализируем индексы для карточек, загруженных из базы данных
    const existingCardElements = cardsList.querySelectorAll('.list-group-item:not(.text-center)');
    existingCardElements.forEach((cardElement, index) => {
        const button = cardElement.querySelector('.remove-card-btn');
        if (button && (!button.dataset.cardIndex || button.dataset.cardIndex === '')) {
            button.dataset.cardIndex = index;
        }
    });

    // Добавляем класс для адаптивности
    const rowElement = document.querySelector('.row');
    if (rowElement) {
        rowElement.classList.add('create-set-row');
    }
});