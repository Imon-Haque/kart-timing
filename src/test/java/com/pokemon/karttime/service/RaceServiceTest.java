package com.pokemon.karttime.service;

import com.pokemon.karttime.dto.CreateRaceRequest;
import com.pokemon.karttime.dto.PassingRequest;
import com.pokemon.karttime.dto.WinnerResponse;
import com.pokemon.karttime.enums.RaceStatus;
import com.pokemon.karttime.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RaceServiceTest {

    @Autowired
    private RaceService raceService;

    @Test
    void sampleCsvProducesKart5AsFastestLapWinner() throws Exception {
        var race = raceService.startRace(new CreateRaceRequest(4, List.of("1", "2", "3", "4", "5")));

        try (InputStream in = new ClassPathResource("sample/karttimes.csv").getInputStream()) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "karttimes.csv",
                    "text/csv",
                    in
            );
            var imported = raceService.importCsv(race.id(), file);
            assertEquals(RaceStatus.FINISHED, imported.raceStatus());
            assertEquals(25, imported.passingsImported());
        }

        WinnerResponse winner = raceService.getWinner(race.id());
        assertEquals("5", winner.kartNumber());
        assertEquals(4, winner.lapNumber());
        assertEquals(56_000L, winner.durationMs());
        assertEquals(LocalTime.of(12, 3, 37), winner.lapStartedAt());
    }

    @Test
    void rejectsNewLapAfterFinishButAllowsInProgressCompletion() {
        var race = raceService.startRace(new CreateRaceRequest(1, List.of("1", "2")));

        raceService.recordPassing(race.id(), new PassingRequest("1", LocalTime.of(12, 0, 0)));
        raceService.recordPassing(race.id(), new PassingRequest("2", LocalTime.of(12, 0, 1)));
        raceService.recordPassing(race.id(), new PassingRequest("1", LocalTime.of(12, 1, 0)));

        raceService.recordPassing(race.id(), new PassingRequest("2", LocalTime.of(12, 0, 50)));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> raceService.recordPassing(race.id(), new PassingRequest("2", LocalTime.of(12, 1, 40)))
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void midnightWrapProducesCorrectDuration() {
        var race = raceService.startRace(new CreateRaceRequest(1, List.of("K1")));
        raceService.recordPassing(race.id(), new PassingRequest("K1", LocalTime.of(23, 59, 35)));
        raceService.recordPassing(race.id(), new PassingRequest("K1", LocalTime.of(0, 0, 25)));

        WinnerResponse winner = raceService.getWinner(race.id());
        assertEquals("K1", winner.kartNumber());
        assertEquals(50_000L, winner.durationMs());
    }

    @Test
    void acceptsAlphanumericKartNumbers() {
        var race = raceService.startRace(new CreateRaceRequest(1, List.of("A12", "B7")));
        raceService.recordPassing(race.id(), new PassingRequest("A12", LocalTime.of(10, 0, 0)));
        raceService.recordPassing(race.id(), new PassingRequest("A12", LocalTime.of(10, 1, 0)));

        WinnerResponse winner = raceService.getWinner(race.id());
        assertEquals("A12", winner.kartNumber());
        assertEquals(60_000L, winner.durationMs());
    }

    @Test
    void manualFinishWhenAlreadyFinishedThrows() {
        var race = raceService.startRace(new CreateRaceRequest(1, List.of("1")));
        raceService.recordPassing(race.id(), new PassingRequest("1", LocalTime.of(12, 0, 0)));
        raceService.recordPassing(race.id(), new PassingRequest("1", LocalTime.of(12, 1, 0)));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> raceService.finishRace(race.id())
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Race is already finished", ex.getMessage());
    }
}
