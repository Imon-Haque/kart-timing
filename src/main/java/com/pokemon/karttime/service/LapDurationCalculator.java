package com.pokemon.karttime.service;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Computes lap duration from clock times only.
 * If finish is not after start (midnight wrap), adds 24 hours.
 */
public final class LapDurationCalculator {

    private LapDurationCalculator() {
    }

    public static long durationMillis(LocalTime startedAt, LocalTime finishedAt) {
        Duration delta = Duration.between(startedAt, finishedAt);
        if (delta.isNegative() || delta.isZero()) {
            delta = delta.plusDays(1);
        }
        return delta.toMillis();
    }
}
