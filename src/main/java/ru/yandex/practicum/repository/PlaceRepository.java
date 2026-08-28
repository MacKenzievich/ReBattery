package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.bot.Place;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

}
