package ru.yandex.practicum.model.bot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ups_id")
    private Ups device;

    private LocalDateTime lastReplaceDate;

    private Long expireMonths;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
}
