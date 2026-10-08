package com.pokemon.karttime.dto;

import com.pokemon.karttime.enums.RaceStatus;

import java.time.LocalTime;

public record PassingResponse(
        Long raceId,
        Long passingId,
        String kartNumber,
        LocalTime passingTime,
        Integer completedLapNumber,
        Long lapDurationMs,
        RaceStatus raceStatus
) {
}
