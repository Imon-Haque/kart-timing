package com.pokemon.karttime.repository;

import com.pokemon.karttime.entity.RaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaceParticipantRepository extends JpaRepository<RaceParticipant, Long> {

    Optional<RaceParticipant> findByRaceIdAndKartNumber(Long raceId, String kartNumber);

    List<RaceParticipant> findByRaceId(Long raceId);
}
