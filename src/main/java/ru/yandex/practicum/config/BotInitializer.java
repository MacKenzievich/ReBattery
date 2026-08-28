package ru.yandex.practicum.config;


import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.yandex.practicum.controller.bot.TelegramBotController;


@Component
public class BotInitializer {

    private final TelegramBotController botController;

    public BotInitializer(TelegramBotController botController) {
        this.botController = botController;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            // Принудительно создаем API сессию Telegram
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            // Регистрируем нашего Long Polling бота
            telegramBotsApi.registerBot(botController);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
