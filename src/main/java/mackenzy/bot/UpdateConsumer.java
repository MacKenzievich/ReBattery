package mackenzy.bot;

import mackenzy.model.AdminStateContext;
import mackenzy.model.State;
import mackenzy.model.User;
import mackenzy.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final BotProperties botProperties;
    private final UserService userService;
    private final Anna anna;
    private Map<Long, AdminStateContext> adminStates = new HashMap<>();

    public UpdateConsumer(BotProperties botProperties, UserService userService, @Lazy Anna anna) {
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
        this.botProperties = botProperties;
        this.userService = userService;
        this.anna = anna;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            String message = update.getMessage().hasText() ? update.getMessage().getText() : "";
            Long chatId = update.getMessage().getChatId();
            if (message.equals("/start")) {
                handleStartMessage(update);
                return;
            } else if (adminStates.containsKey(chatId)) {
                handleAdminInputNewUser(message, chatId);

            }
        }

        if (update.hasCallbackQuery()) {
            handleCallBackQuery(update.getCallbackQuery());

        }
    }

    private void handleAdminInputNewUser(String message, Long chatId) {
        State currentState = adminStates.get(chatId).getState();
        if (currentState.equals(State.WAITING_FOR_USER_ID)) {
            try {
                Long userId = Long.valueOf(message);
                adminStates.put(chatId, new AdminStateContext(State.WAITING_FOR_PSEUDONYM, userId));
                userService.addUser(userId, null);
                sendTextMessage(chatId, "Введите имя пользователя.");
            } catch (NumberFormatException e) {
                sendTextMessage(chatId, "Id должен состоять только из цифр!");
            }
        } else if (currentState.equals(State.WAITING_FOR_PSEUDONYM)) {
            String name = message;
            Long userId = adminStates.get(chatId).getUserId();
            userService.addUser(userId, name);
            if (isUser(userId)) {
                sendTextMessage(chatId, "Пользователь успешно добавлен!");
                adminStates.remove(chatId);
            }
        }
    }

    private void handleCallBackQuery(CallbackQuery callbackQuery) {
        String callBackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        removeInlineKeyboard(chatId, messageId);
        switch (callBackData) {
            case "add_user" -> handleAddUserCallBack();
            case "delete_user" -> remoteUserSearch();
            case "send_love_message" -> sendLoveMessage();
        }

    }

    private void handleAddUserCallBack() {
        sendTextMessage(botProperties.getAdminId(), "Введите id пользователя");
        adminStates.put(botProperties.getAdminId(), new AdminStateContext(State.WAITING_FOR_USER_ID, null));
    }


    private void sendLoveMessage() {
        anna.sendLoveMessage();
    }

    private void remoteUserSearch() {
    }


    private void handleStartMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();
        if (isAdmin(chatId)) {
            sendTextMessage(chatId, "Привет, " + firstName + "!");
            sendAdminMenu(chatId);
        } else if (isUser(chatId)) {
            sendTextMessage(chatId, "Приветствую, " + firstName);
            sendMenu(chatId);
        } else {
            sendTextMessage(chatId, "Извините. У вас нет доступа. Обратитесь к Mackenzievich");
            sendTextMessage(botProperties.getAdminId(), firstName + " хочет к нам в друзья! user_id ");
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

    private void removeInlineKeyboard(Long chatId, Integer messageId) {
        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(List.of()).build())
                .build();
        try {
            telegramClient.execute(edit);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAdmin(Long chatId) {
        return botProperties.getAdminId().equals(chatId);
    }

    private boolean isUser(Long chatId) {
        return userService.isUser(chatId);
    }


}