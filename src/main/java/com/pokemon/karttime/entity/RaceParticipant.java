package com.pokemon.karttime.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "race_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"race_id", "kart_number"})
)
@Getter
@Setter
public class RaceParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    /** Alphanumeric kart identifier (e.g. "5", "A12"). */
    @Column(name = "kart_number", nullable = false, length = 32)
    private String kartNumber;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passing> passings = new ArrayList<>();

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lap> laps = new ArrayList<>();
}
