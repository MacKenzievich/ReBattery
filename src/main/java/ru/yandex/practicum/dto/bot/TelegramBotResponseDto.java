package ru.yandex.practicum.dto.bot;

import lombok.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelegramBotResponseDto {
    private String text;
    private ReplyKeyboard keyboard;
}
