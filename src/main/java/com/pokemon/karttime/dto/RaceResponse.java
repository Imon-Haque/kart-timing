package com.pokemon.karttime.dto;

import com.pokemon.karttime.enums.RaceStatus;

import java.time.LocalTime;
import java.util.List;

public record RaceResponse(
        Long id,
        RaceStatus status,
        int configuredLaps,
        LocalTime startedAt,
        LocalTime finishedAt,
        List<String> kartNumbers,
        List<LapSummary> laps
) {
    public record LapSummary(
            String kartNumber,
            int lapNumber,
            LocalTime startedAt,
            LocalTime finishedAt,
            Long durationMs,
            boolean pending
    ) {
    }
}
