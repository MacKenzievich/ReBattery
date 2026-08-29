package mackenzy.bot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class BotProperties {

    private String name;

    private String token;

    private Long adminId;

}