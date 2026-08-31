package mackenzy.bot;

import mackenzy.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;



@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final BotProperties botProperties;
    private final UserService userService;
    private final Anna anna;


    public UpdateConsumer(BotProperties botProperties, UserService userService,@Lazy Anna anna) {
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
        this.botProperties = botProperties;
        this.userService = userService;
        this.anna = anna;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String lastName = update.getMessage().getFrom().getLastName();
            if (message.equals("/start")) {
                handleStartMessage(chatId, lastName);
            }
        }

        if (update.hasCallbackQuery()) {
            handleCallBackQuery(update.getCallbackQuery());

        }
    }

    private void handleCallBackQuery(CallbackQuery callbackQuery) {
        String callBackData = callbackQuery.getData();

        switch (callBackData) {
            case "add_user" -> buildNewUser();
            case "delete_user" -> remoteUserSearch();
            case "send_love_message" -> sendLoveMessage();
        }

    }

    private void sendLoveMessage() {
        anna.sendLoveMessage();
    }

    private void remoteUserSearch() {
    }

    private void buildNewUser() {
    }

    private void handleStartMessage(Long chatId, String lastName) {
        if (chatId.equals(botProperties.getAdminId())) {
            sendTextMessage(chatId, "Привет " + lastName + "!" );
            sendAdminMenu(chatId);
        } else if (userService.isUser(chatId)) {
            sendTextMessage(chatId, "Приветсвую " + lastName);
            sendMenu(chatId);
        } else {
            sendTextMessage(chatId, "Извините. У вас нет доступа. Обратитесь к Mackenzievich");
            sendTextMessage(botProperties.getAdminId(), "Кто-то хочет к нам в друзья! Его user id ");
            sendTextMessage(botProperties.getAdminId(), "" + chatId);
        }
    }


    public void sendTextMessage(Long chatId, String text) { //сделать приватным
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
                .text("Что желаешь сделать?")
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

        var button5 = InlineKeyboardButton.builder()
                .text("Отправить Ане сообщение")
                .callbackData("send_love_message")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(button1),
                new InlineKeyboardRow(button2),
                new InlineKeyboardRow(button3),
                new InlineKeyboardRow(button4),
                new InlineKeyboardRow(button5)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);
        message.setReplyMarkup(markup);
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMenu(Long chatId) {

    }


}