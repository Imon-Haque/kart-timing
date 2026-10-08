package com.pokemon.karttime.repository;

import com.pokemon.karttime.entity.Race;
import com.pokemon.karttime.enums.RaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RaceRepository extends JpaRepository<Race, Long> {

    boolean existsByStatus(RaceStatus status);

    Optional<Race> findFirstByOrderByIdDesc();

    Optional<Race> findFirstByStatusOrderByIdDesc(RaceStatus status);
}
