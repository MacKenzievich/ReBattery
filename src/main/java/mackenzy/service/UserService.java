package mackenzy.service;

import mackenzy.model.User;

public interface UserService {
    void addUser(User user);
    void deleteUser(Long userId);
    boolean isUser(Long userId);
}
