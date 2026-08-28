package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.bot.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
