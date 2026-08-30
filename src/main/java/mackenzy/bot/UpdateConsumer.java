package mackenzy.bot;

import mackenzy.model.User;
import mackenzy.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component

public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final BotProperties botProperties;
    private final UserRepository userRepository;

    public UpdateConsumer(BotProperties botProperties, UserRepository userRepository) {
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
        this.botProperties = botProperties;
        this.userRepository = userRepository;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            String message = update.getMessage().getText();
            System.out.println(message);
            Long chatId = update.getMessage().getChatId();
            Optional<User> optionalUser  = userRepository.findById(chatId);
            if (message.equals("/start")) {
                if (chatId.equals(botProperties.getAdminId())) {
                    sendTextMessage(chatId, "Привет хозяин!");
                    sendAdminMenu(chatId);
                } else if (optionalUser.isPresent()) {
                    sendTextMessage(chatId, "Приветсвую " + optionalUser.get().getName());
                    sendMenu(chatId);
                } else {
                    sendTextMessage(chatId, "Извините. У вас нет доступа. Обратитесь к Mackenzievich");
                    sendTextMessage(botProperties.getAdminId(), "Кто-то хочет к нам в друзья! Его user id " +
                            chatId);
                }
            }
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId.toString())
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAdminMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Что желаешь сделать")
                .chatId(chatId.toString())
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Добавить user")
                .callbackData("add_user")
                .build();

        var button2 = InlineKeyboardButton.builder()
                .text("Удалить user")
                .callbackData("delete_user")
                .build();

        var button3 = InlineKeyboardButton.builder()
                .text("Отметить замену батареи")
                .callbackData("mark_change_battery")
                .build();

        var button4 = InlineKeyboardButton.builder()
                .text("Просмотреть ближайшие замены")
                .callbackData("view_upcoming_ubstitutions")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(button1),
                new InlineKeyboardRow(button2),
                new InlineKeyboardRow(button3),
                new InlineKeyboardRow(button4)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);
        message.setReplyMarkup(markup);
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMenu(Long chatId){

    }

    @Scheduled(fixedRate = 60000)
    public void adminMesssage(){
        sendTextMessage(botProperties.getAdminId(), "Сейчас: "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }
}