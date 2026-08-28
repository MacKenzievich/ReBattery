package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import ru.yandex.practicum.dto.bot.TelegramBotResponseDto;

import ru.yandex.practicum.model.bot.BotState;
import ru.yandex.practicum.model.bot.User;
import ru.yandex.practicum.repository.PlaceRepository;
import ru.yandex.practicum.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBotService {

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final long adminId;


    private BotState adminState = BotState.IDLE;

    public TelegramBotService(UserRepository userRepository,
                              PlaceRepository placeRepository,
                              @Value("${telegram.bot.admin-id}") long adminId) {
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
        this.adminId = adminId;
    }

    @Transactional
    public TelegramBotResponseDto generateResponse(long userId, String messageText) {
        boolean isAdmin = (userId == adminId);

        // 1. БЕЗОПАСНОСТЬ: Если пишет не админ, проверяем его наличие в базе доступов PostgreSQL
        if (!isAdmin) {
            boolean isAllowed = userRepository.existsById(userId);
            if (!isAllowed) {
                return TelegramBotResponseDto.builder()
                        .text("⛔️ Доступ ограничен. Ваш User ID (" + userId + ") не авторизован в системе ReBattery.")
                        .build();
            }
        }

        // 2. ОБРАБОТКА КОМАНДЫ /START (Выдача правильных кнопок)
        if ("/start".equals(messageText)) {
            if (isAdmin) {
                adminState = BotState.IDLE;
                return TelegramBotResponseDto.builder()
                        .text("Привет, Создатель! 👑 Тебе доступны ВСЕ функции. Твоё полное меню:")
                        .keyboard(createAdminMenu()) // Вам — 4 кнопки (управление юзерами + приборы)
                        .build();
            } else {
                // Регистрируем/обновляем одобренного пользователя в базе, если его еще нет
                if (!userRepository.existsById(userId)) {
                    userRepository.save(new User(userId, "Authorized User"));
                }
                return TelegramBotResponseDto.builder()
                        .text("Привет! Твой доступ подтвержден администратором 🔐. Твоё меню:")
                        .keyboard(createUserMenu()) // Им — только 2 кнопки (работа с приборами)
                        .build();
            }
        }

        // =========================================================================
        // 3. УНИКАЛЬНЫЕ МЕТОДЫ АДМИНИСТРАТОРА (Доступны и видны ТОЛЬКО вам)
        // =========================================================================
        if (isAdmin) {
            if ("➕ Добавить пользователя".equals(messageText)) {
                adminState = BotState.WAITING_USER_ADD; // Включаем режим ожидания числового ID
                return TelegramBotResponseDto.builder()
                        .text("Режим админки. Введите числовой **User ID** нового пользователя:")
                        .build();
            }

            if ("❌ Удалить пользователя".equals(messageText)) {
                adminState = BotState.WAITING_USER_DELETE; // Включаем режим ожидания удаления
                return TelegramBotResponseDto.builder()
                        .text("Режим админки. Введите **User ID** пользователя для удаления из базы:")
                        .build();
            }

            // Пошаговый прием введенного вами ID с клавиатуры
            if (adminState == BotState.WAITING_USER_ADD) {
                adminState = BotState.IDLE; // Сбрасываем режим
                try {
                    long targetUserId = Long.parseLong(messageText.trim());
                    if (userRepository.existsById(targetUserId) || targetUserId == adminId) {
                        return TelegramBotResponseDto.builder().text("ℹ️ Этот пользователь уже есть в белом списке.").build();
                    }
                    userRepository.save(new User(targetUserId, "User_" + targetUserId)); // Добавляем в базу доступов
                    return TelegramBotResponseDto.builder().text("✅ Отлично! Пользователь " + targetUserId + " добавлен. Теперь он сможет нажать /start и пользоваться ботом.").build();
                } catch (NumberFormatException e) {
                    return TelegramBotResponseDto.builder().text("⚠️ Ошибка. Введите корректное число ID.").build();
                }
            }

            if (adminState == BotState.WAITING_USER_DELETE) {
                adminState = BotState.IDLE;
                try {
                    long targetUserId = Long.parseLong(messageText.trim());
                    if (userRepository.existsById(targetUserId)) {
                        userRepository.deleteById(targetUserId); // Полностью стираем из PostgreSQL
                        return TelegramBotResponseDto.builder().text("❌ Пользователь " + targetUserId + " успешно удален. Доступ для него закрыт.").build();
                    } else {
                        return TelegramBotResponseDto.builder().text("⚠️ Этот ID не найден в базе данных.").build();
                    }
                } catch (NumberFormatException e) {
                    return TelegramBotResponseDto.builder().text("⚠️ Ошибка. Введите корректное число ID.").build();
                }
            }
        }

        // =========================================================================
        // 4. ОБЩИЕ ФУНКЦИИ ДЛЯ ВСЕХ АВТОРИЗОВАННЫХ (Доступны И ВАМ, и ИМ)
        // =========================================================================
        if ("➕ Добавить прибор".equals(messageText)) {
            // В будущем здесь включится пошаговый стейт WAITING_DEVICE_PLACE
            return TelegramBotResponseDto.builder()
                    .text("Вы запустили процесс добавления устройства. Где оно находится (например: Кухня)?")
                    .build();
        }

        if ("📋 Мои приборы".equals(messageText)) {
            return TelegramBotResponseDto.builder()
                    .text("Вот список ваших приборов из базы данных PostgreSQL...")
                    .build();
        }

        // Заглушка, если введен просто текст
        return TelegramBotResponseDto.builder()
                .text("Используйте кнопки меню для управления.")
                .build();
    }

    /**
     * ВАШЕ ПОЛНОЕ МЕНЮ (4 кнопки — 2 строки)
     */
    private ReplyKeyboardMarkup createAdminMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        // Первая строка — общие функции приборов (вы их тоже видите и можете нажимать!)
        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Добавить прибор");
        row1.add("📋 Мои приборы");

        // Вторая строка — ваши уникальные админские кнопки
        KeyboardRow row2 = new KeyboardRow();
        row2.add("➕ Добавить пользователя");
        row2.add("❌ Удалить пользователя");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * ИХ ПРОСТОЕ МЕНЮ (Только 2 кнопки — 1 строка)
     */
    private ReplyKeyboardMarkup createUserMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        KeyboardRow row = new KeyboardRow();
        row.add("➕ Добавить прибор");
        row.add("📋 Мои приборы");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
