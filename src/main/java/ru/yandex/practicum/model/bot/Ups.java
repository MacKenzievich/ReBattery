package ru.yandex.practicum.model.bot;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "upses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ups {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String modelName;

    private Integer batteryCount;
}

