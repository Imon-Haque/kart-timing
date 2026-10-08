package com.pokemon.karttime.repository;

import com.pokemon.karttime.entity.Lap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LapRepository extends JpaRepository<Lap, Long> {

    Optional<Lap> findByParticipantIdAndFinishedAtIsNull(Long participantId);

    long countByParticipantIdAndFinishedAtIsNotNull(Long participantId);

    @Query("""
            select l from Lap l
            where l.participant.race.id = :raceId
            order by l.participant.kartNumber asc, l.lapNumber asc
            """)
    List<Lap> findByRaceIdOrderByKartNumberAscLapNumberAsc(Long raceId);

    @Query("""
            select l from Lap l
            where l.participant.race.id = :raceId
              and l.finishedAt is not null
            order by l.durationMs asc, l.startedAt asc
            """)
    List<Lap> findCompletedFastestLaps(Long raceId);

    default Optional<Lap> findFastestLap(Long raceId) {
        List<Lap> laps = findCompletedFastestLaps(raceId);
        return laps.isEmpty() ? Optional.empty() : Optional.of(laps.getFirst());
    }
}
