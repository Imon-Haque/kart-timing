package com.pokemon.karttime.entity;

import com.pokemon.karttime.enums.RaceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "races")
@Getter
@Setter
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RaceStatus status;

    @Column(name = "configured_laps", nullable = false)
    private int configuredLaps;

    @Column(name = "started_at", nullable = false)
    private LocalTime startedAt;

    @Column(name = "finished_at")
    private LocalTime finishedAt;

    @OneToMany(mappedBy = "race", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaceParticipant> participants = new ArrayList<>();

    public void addParticipant(RaceParticipant participant) {
        participants.add(participant);
        participant.setRace(this);
    }
}
