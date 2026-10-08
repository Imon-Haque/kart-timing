package com.pokemon.karttime.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(
        name = "laps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "lap_number"})
)
@Getter
@Setter
public class Lap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private RaceParticipant participant;

    @Column(name = "lap_number", nullable = false)
    private int lapNumber;

    @Column(name = "started_at", nullable = false)
    private LocalTime startedAt;

    /** Null while the lap is still in progress (pending). */
    @Column(name = "finished_at")
    private LocalTime finishedAt;

    /** Null while the lap is still pending. */
    @Column(name = "duration_ms")
    private Long durationMs;

    public boolean isPending() {
        return finishedAt == null;
    }
}
