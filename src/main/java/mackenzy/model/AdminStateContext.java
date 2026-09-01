package mackenzy.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AdminStateContext {
    private State state;
    private Long userId;
}
