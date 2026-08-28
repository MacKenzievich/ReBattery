package ru.yandex.practicum.model.bot;

public enum BotState {
    IDLE,                 // Режим покоя (обычный диалог)
    WAITING_USER_ADD,     // Админ вводит ID нового пользователя
    WAITING_USER_DELETE,  // Админ вводит ID для удаления пользователя
    WAITING_DEVICE_PLACE, // Юзер/Админ вводит место (например, Кухня)
    WAITING_DEVICE_NAME   // Юзер/Админ вводит название прибора (например, Часы)
}
