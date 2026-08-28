package ru.yandex.practicum.controller.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final String botUsername;

    public TelegramBotController(@Value("${telegram.bot.token}") String botToken,
                         @Value("${telegram.bot.name}") String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что событие — это текстовое сообщение в чате
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Распределяем команды
            switch (messageText) {
                case "/start":
                    sendText(chatId, "Привет! Я проект ReBattery 🔋. Я помогу тебе вовремя менять батарейки.");
                    break;
                case "/add":
                    sendText(chatId, "Вы запустили процесс добавления устройства. (Здесь будет пошаговая логика)");
                    break;
                default:
                    sendText(chatId, "Я получил твой текст: \"" + messageText + "\", но пока не знаю, что с ним делать.");
                    break;
            }
        }
    }

    /**
     * Универсальный метод для отправки текстовых ответов пользователю.
     * Мы сможем вызывать его из любой точки приложения (например, из планировщика напоминаний).
     */
    public void sendText(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
        try {
            execute(message); // Отправка HTTP POST запроса на сервера Telegram
        } catch (TelegramApiException e) {
            // В будущем заменим на логгер slf4j, пока выводим стек ошибок в консоль Docker
            e.printStackTrace();
        }
    }
}

