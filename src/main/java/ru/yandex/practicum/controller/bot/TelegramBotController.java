package ru.yandex.practicum.controller.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.yandex.practicum.dto.bot.TelegramBotResponseDto;
import ru.yandex.practicum.service.TelegramBotService;


@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final String botUsername;
    private final TelegramBotService telegramBotService;

    public TelegramBotController(@Value("${telegram.bot.token}") String botToken,
                                 @Value("${telegram.bot.name}") String botUsername,
                                 TelegramBotService telegramBotService) {
        super(botToken);
        this.botUsername = botUsername;
        this.telegramBotService = telegramBotService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            TelegramBotResponseDto telegramBotResponseDto = telegramBotService.generateResponse(userId, messageText) ;
            sendResponse(chatId, telegramBotResponseDto);
        }
    }

    private void sendResponse(long chatId, TelegramBotResponseDto telegramBotResponseDto) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(telegramBotResponseDto.getText())
                .parseMode("Markdown")
                .build();

        if (telegramBotResponseDto.getKeyboard() != null) {
            message.setReplyMarkup(telegramBotResponseDto.getKeyboard());
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
