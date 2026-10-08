package com.pokemon.karttime.dto;

import java.time.LocalTime;

public record WinnerResponse(
        Long raceId,
        String kartNumber,
        Integer lapNumber,
        Long durationMs,
        LocalTime lapStartedAt,
        LocalTime lapFinishedAt
) {
}
