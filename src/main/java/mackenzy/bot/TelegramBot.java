package mackenzy.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;


@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBot implements SpringLongPollingBot {

    private final BotProperties botProperties;
    private final UpdateConsumer updateConsumer;

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
