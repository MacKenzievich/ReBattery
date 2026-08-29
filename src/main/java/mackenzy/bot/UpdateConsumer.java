package mackenzy.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;

    public UpdateConsumer(BotProperties botProperties) {
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
    }

    @Override
    public void consume(Update update) {
        var userID = update.getMessage().getChatId();
        String message = update.getMessage().getText();
        SendMessage sendMessage = SendMessage.builder()
                .text("Пришло сообщение от " + userID + " c текстом " + message.toLowerCase())
                .chatId(update.getMessage().getChatId())
                .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
