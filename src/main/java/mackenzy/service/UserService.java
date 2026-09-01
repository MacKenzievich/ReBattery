package mackenzy.service;

import mackenzy.model.User;

import java.util.List;

public interface UserService {
    void addUser(Long userId, String name);
    void deleteUser(Long userId);
    boolean isUser(Long userId);
    List<User> findAllUsers();
}
