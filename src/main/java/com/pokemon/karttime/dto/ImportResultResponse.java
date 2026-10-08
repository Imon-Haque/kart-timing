package com.pokemon.karttime.dto;

import com.pokemon.karttime.enums.RaceStatus;

public record ImportResultResponse(
        Long raceId,
        int passingsImported,
        RaceStatus raceStatus
) {
}
