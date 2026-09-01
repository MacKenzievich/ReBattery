package mackenzy.service;

import mackenzy.model.User;

public interface UserService {
    void addUser(Long userId, String name);
    void deleteUser(Long userId);
    boolean isUser(Long userId);
}
