package com.pokemon.karttime.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LapDurationCalculatorTest {

    @Test
    void sameDayLap() {
        long ms = LapDurationCalculator.durationMillis(
                LocalTime.of(12, 0, 0),
                LocalTime.of(12, 1, 10)
        );
        assertEquals(70_000L, ms);
    }

    @Test
    void midnightWrapLap() {
        long ms = LapDurationCalculator.durationMillis(
                LocalTime.of(23, 59, 35),
                LocalTime.of(0, 0, 25)
        );
        assertEquals(50_000L, ms);
    }
}
