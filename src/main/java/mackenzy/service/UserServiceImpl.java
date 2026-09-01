package mackenzy.service;

import lombok.RequiredArgsConstructor;
import mackenzy.model.User;
import mackenzy.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void addUser(Long userId, String name) {
        userRepository.save(new User(userId, name));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public boolean isUser(Long userId) {
        return userRepository.existsById(userId);
    }
}
