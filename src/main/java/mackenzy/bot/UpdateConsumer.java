package mackenzy.bot;

import mackenzy.model.AdminStateContext;
import mackenzy.model.ArSt;
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
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;


@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final BotProperties botProperties;
    private final UserService userService;
    private final Anna anna;
    private Map<Long, AdminStateContext> adminStates = new HashMap<>();
    private ArSt a;

    private boolean isMyPoint = false;
    private boolean isEnemyPoint = false;

    private Double myPointX;
    private Double myPointY;
    private Double enemyPointX;
    private Double enemyPointY;

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
                handleAdminStates(message, chatId);
            } else if (a != null) {
                try {
                    handleInputDistance(chatId, message);
                } catch (NumberFormatException e) {
                    sendTextMessage(chatId, "Координаты должны быть числом!");
                }
            }
        }

        if (update.hasCallbackQuery()) {
            handleCallBackQuery(update.getCallbackQuery());

        }
    }


    private void handleInputDistance(Long chatId, String message) {
        if (a.equals(ArSt.WAIT_MY_X)) {
            myPointX = Double.parseDouble(message);
            sendTextMessage(chatId, "Введите координату Y");
            a = ArSt.WAIT_MY_Y;
        } else if (a.equals(ArSt.WAIT_MY_Y)) {
            myPointY = Double.parseDouble(message);
            sendTextMessage(chatId, "Ваши координаты X: " + myPointX + ", Y: " + myPointY);
            sendMenu(chatId);
        } else if (a.equals(ArSt.WAIT_EN_X)) {
            enemyPointX = Double.parseDouble(message);
            a = ArSt.WAIT_EN_Y;
            sendTextMessage(chatId, "Введите координату Y");
        } else if (a.equals(ArSt.WAIT_EN_Y)) {
            enemyPointY = Double.parseDouble(message);
            sendTextMessage(chatId, "Вражеские координаты Х: " + enemyPointX + ", Y: " + enemyPointY);
            sendMenu(chatId);
        }
    }

    private void handleCalc(Long chatId) {
        double deltaX = enemyPointX - myPointX;
        double deltaY = enemyPointY - myPointY;
        double deltaDistance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        long realDistance = Math.round(deltaDistance * 100.0);
        sendTextMessage(chatId, "Раccтояние до цели:  " + realDistance + " метров.");
        sendMenu(chatId);
    }

    private void handleAdminStates(String message, Long chatId) {
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
            } else {
                sendTextMessage(chatId, "Что-то пошло не так!");
            }
            sendAdminMenu(chatId);
        } else if (currentState.equals(State.WAITING_FOR_DELETE_ID)) {
            try {
                Long userId = Long.valueOf(message);
                userService.deleteUser(userId);
                if (!isUser(userId)) {
                    sendTextMessage(chatId, "Пользователь успешно удален.");
                    adminStates.remove(chatId);
                }
            } catch (NumberFormatException e) {
                sendTextMessage(chatId, "Id должен состоять только из цифр!");
            }
            sendAdminMenu(chatId);
        }

    }

    private void handleCallBackQuery(CallbackQuery callbackQuery) {
        String callBackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        removeInlineKeyboard(chatId, messageId);
        switch (callBackData) {
            case "add_user" -> handleAddUserCallBack();
            case "delete_user" -> handleDeleteUser();
            case "show_users" -> handleShowUsersCallBack();
            case "my_point" -> handleMyPoint(chatId);
            case "enemy_point" -> handleEnemyPoint(chatId);
            case "calc" -> handleCalc(chatId);
        }

    }

    private void handleEnemyPoint(Long chatId) {
        a = ArSt.WAIT_EN_X;
        sendTextMessage(chatId, "Введите координату X");
    }

    private void handleMyPoint(Long chatId) {
        a = ArSt.WAIT_MY_X;
        sendTextMessage(chatId, "Введите координату X");
    }

    private void handleShowUsersCallBack() {
        List<User> users = userService.findAllUsers();
        StringBuilder sb = new StringBuilder("📋 Список всех пользователей:\n\n");
        for (User user : users) {
            sb.append("🆔 `").append(user.getId()).append("` — ")
                    .append(user.getPseudonym() != null ? user.getPseudonym() : "Без имени").append("\n");
        }
        sendTextMessage(botProperties.getAdminId(), sb.toString());
        sendAdminMenu(botProperties.getAdminId());
    }

    private void handleAddUserCallBack() {
        sendTextMessage(botProperties.getAdminId(), "Введите id пользователя");
        adminStates.put(botProperties.getAdminId(), new AdminStateContext(State.WAITING_FOR_USER_ID, null));
    }


    private void sendLoveMessage() {
        anna.sendLoveMessage();
    }

    private void handleDeleteUser() {
        sendTextMessage(botProperties.getAdminId(), "Введите id пользователя, которого нужно удалить.");
        adminStates.put(botProperties.getAdminId(), new AdminStateContext(State.WAITING_FOR_DELETE_ID, null));
    }


    private void handleStartMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();
        if (isAdmin(chatId)) {
            sendAdminMenu(chatId);
            sendMenu(chatId);
        } else if (isUser(chatId)) {
            sendTextMessage(chatId, "Приветствую, " + firstName);
            sendMenu(chatId);
        } else {
            sendTextMessage(chatId, "Извините. У вас нет доступа. Обратитесь к Mackenzievich");
            sendTextMessage(botProperties.getAdminId(), firstName + " зашел в бот впервые! ID пользователя:");
            sendTextMessage(botProperties.getAdminId(), "" + chatId);
        }
    }


    public void sendTextMessage(Long chatId, String text) { //сделать приватным
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId.toString())
                .parseMode("Markdown")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAdminMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("admin menu")
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
                .text("Показать список пользователей")
                .callbackData("show_users")
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
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Меню артиллериста")
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Установить свои координаты.")
                .callbackData("my_point")
                .build();

        var button2 = InlineKeyboardButton.builder()
                .text("Установить координаты цели.")
                .callbackData("enemy_point")
                .build();
        var button3 = InlineKeyboardButton.builder()
                .text("Рассчитать расстояние")
                .callbackData("calc")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(button1),
                new InlineKeyboardRow(button2),
                new InlineKeyboardRow(button3));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);
        message.setReplyMarkup(markup);
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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