package com.pokemon.karttime.service;

import com.pokemon.karttime.dto.CreateRaceRequest;
import com.pokemon.karttime.dto.ImportResultResponse;
import com.pokemon.karttime.dto.PassingRequest;
import com.pokemon.karttime.dto.PassingResponse;
import com.pokemon.karttime.dto.RaceResponse;
import com.pokemon.karttime.dto.WinnerResponse;
import com.pokemon.karttime.entity.Lap;
import com.pokemon.karttime.entity.Passing;
import com.pokemon.karttime.entity.Race;
import com.pokemon.karttime.entity.RaceParticipant;
import com.pokemon.karttime.enums.RaceStatus;
import com.pokemon.karttime.exception.ApiException;
import com.pokemon.karttime.repository.LapRepository;
import com.pokemon.karttime.repository.PassingRepository;
import com.pokemon.karttime.repository.RaceParticipantRepository;
import com.pokemon.karttime.repository.RaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class RaceService {

    private static final Logger log = LoggerFactory.getLogger(RaceService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm:ss");

    private final RaceRepository raceRepository;
    private final RaceParticipantRepository participantRepository;
    private final PassingRepository passingRepository;
    private final LapRepository lapRepository;

    public RaceService(
            RaceRepository raceRepository,
            RaceParticipantRepository participantRepository,
            PassingRepository passingRepository,
            LapRepository lapRepository
    ) {
        this.raceRepository = raceRepository;
        this.participantRepository = participantRepository;
        this.passingRepository = passingRepository;
        this.lapRepository = lapRepository;
    }

    @Transactional
    public RaceResponse startRace(CreateRaceRequest request) {
        if (raceRepository.existsByStatus(RaceStatus.STARTED)) {
            throw new ApiException(HttpStatus.CONFLICT, "An active race is already running");
        }

        List<String> normalized = request.kartNumbers().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        Set<String> unique = new HashSet<>(normalized);
        if (unique.size() != normalized.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Kart numbers must be unique");
        }
        if (unique.isEmpty() || unique.size() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A race needs between 1 and 5 karts");
        }

        Race race = new Race();
        race.setStatus(RaceStatus.STARTED);
        race.setConfiguredLaps(request.lapCount());
        race.setStartedAt(LocalTime.now().withNano(0));

        unique.stream().sorted().forEach(kartNumber -> {
            RaceParticipant participant = new RaceParticipant();
            participant.setKartNumber(kartNumber);
            race.addParticipant(participant);
        });

        Race saved = raceRepository.save(race);
        log.info("Race {} started with {} laps for karts {}",
                saved.getId(), saved.getConfiguredLaps(), unique);
        return toRaceResponse(saved);
    }

    @Transactional
    public PassingResponse recordPassing(Long raceId, PassingRequest request) {
        Race race = getRace(raceId);
        String kartNumber = request.kartNumber().trim();
        RaceParticipant participant = participantRepository
                .findByRaceIdAndKartNumber(raceId, kartNumber)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Kart " + kartNumber + " is not registered in this race"
                ));

        Optional<Lap> pendingLap = lapRepository.findByParticipantIdAndFinishedAtIsNull(participant.getId());
        validateRaceAcceptsPassing(race, pendingLap.isPresent());

        Passing passing = new Passing();
        passing.setParticipant(participant);
        passing.setPassingTime(request.passingTime());
        passingRepository.save(passing);
        log.debug("Passing recorded raceId={} kart={} time={}", raceId, kartNumber, request.passingTime());

        Integer completedLapNumber = null;
        Long lapDurationMs = null;

        if (pendingLap.isEmpty()) {
            // First crossing (or start of racing): open lap 1
            openPendingLap(participant, 1, request.passingTime());
        } else {
            Lap lap = pendingLap.get();
            lap.setFinishedAt(request.passingTime());
            lap.setDurationMs(LapDurationCalculator.durationMillis(lap.getStartedAt(), request.passingTime()));
            lapRepository.save(lap);

            completedLapNumber = lap.getLapNumber();
            lapDurationMs = lap.getDurationMs();
            log.debug("Lap {} completed for kart {} in {} ms",
                    completedLapNumber, kartNumber, lapDurationMs);

            long completed = lapRepository.countByParticipantIdAndFinishedAtIsNotNull(participant.getId());
            if (race.getStatus() == RaceStatus.STARTED && completed >= race.getConfiguredLaps()) {
                finishRaceInternal(race, request.passingTime());
            }

            // Only start another lap while the race is still running
            if (race.getStatus() == RaceStatus.STARTED) {
                openPendingLap(participant, (int) completed + 1, request.passingTime());
            }
        }

        return new PassingResponse(
                race.getId(),
                passing.getId(),
                kartNumber,
                request.passingTime(),
                completedLapNumber,
                lapDurationMs,
                race.getStatus()
        );
    }

    @Transactional
    public ImportResultResponse importCsv(Long raceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV file is required");
        }

        int imported = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (first && line.toLowerCase(Locale.ROOT).startsWith("kart")) {
                    first = false;
                    continue;
                }
                first = false;
                String[] parts = line.split(",", 2);
                if (parts.length < 2) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid CSV row: " + line);
                }
                String kartNumber = parts[0].trim();
                LocalTime passingTime;
                try {
                    if (kartNumber.isEmpty()) {
                        throw new IllegalArgumentException("empty kart");
                    }
                    passingTime = LocalTime.parse(parts[1].trim(), TIME_FORMAT);
                } catch (RuntimeException ex) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid CSV row: " + line);
                }
                recordPassing(raceId, new PassingRequest(kartNumber, passingTime));
                imported++;
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read CSV: " + ex.getMessage());
        }

        Race race = getRace(raceId);
        log.info("Imported {} passings into race {} (status={})", imported, raceId, race.getStatus());
        return new ImportResultResponse(raceId, imported, race.getStatus());
    }

    @Transactional
    public RaceResponse finishRace(Long raceId) {
        Race race = getRace(raceId);
        if (race.getStatus() == RaceStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Race is already finished");
        }
        finishRaceInternal(race, LocalTime.now().withNano(0));
        log.info("Race {} finished manually", raceId);
        return toRaceResponse(race);
    }

    @Transactional(readOnly = true)
    public RaceResponse getRaceResponse(Long raceId) {
        return toRaceResponse(getRace(raceId));
    }

    @Transactional(readOnly = true)
    public WinnerResponse getLatestWinner() {
        Race race = raceRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No races found"));
        return getWinnerForRace(race);
    }

    @Transactional(readOnly = true)
    public WinnerResponse getWinner(Long raceId) {
        return getWinnerForRace(getRace(raceId));
    }

    private WinnerResponse getWinnerForRace(Race race) {
        if (race.getStatus() != RaceStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Race is not finished yet");
        }
        Lap fastest = lapRepository.findFastestLap(race.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No completed laps for this race"));

        return new WinnerResponse(
                race.getId(),
                fastest.getParticipant().getKartNumber(),
                fastest.getLapNumber(),
                fastest.getDurationMs(),
                fastest.getStartedAt(),
                fastest.getFinishedAt()
        );
    }

    private void openPendingLap(RaceParticipant participant, int lapNumber, LocalTime startedAt) {
        Lap lap = new Lap();
        lap.setParticipant(participant);
        lap.setLapNumber(lapNumber);
        lap.setStartedAt(startedAt);
        lap.setFinishedAt(null);
        lap.setDurationMs(null);
        lapRepository.save(lap);
    }

    private void finishRaceInternal(Race race, LocalTime finishedAt) {
        race.setStatus(RaceStatus.FINISHED);
        race.setFinishedAt(finishedAt);
        raceRepository.save(race);
        log.info("Race {} finished at {}", race.getId(), finishedAt);
    }

    /**
     * STARTED: always accept.
     * FINISHED: accept only if the kart has a pending lap to close (README checkered-flag rule).
     */
    private void validateRaceAcceptsPassing(Race race, boolean hasPendingLap) {
        if (race.getStatus() == RaceStatus.STARTED) {
            return;
        }
        if (race.getStatus() == RaceStatus.FINISHED && hasPendingLap) {
            return;
        }
        throw new ApiException(
                HttpStatus.CONFLICT,
                "Race is finished; only an in-progress lap may still be completed"
        );
    }

    private Race getRace(Long raceId) {
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Race not found: " + raceId));
    }

    private RaceResponse toRaceResponse(Race race) {
        List<String> kartNumbers = participantRepository.findByRaceId(race.getId()).stream()
                .map(RaceParticipant::getKartNumber)
                .sorted(Comparator.naturalOrder())
                .toList();

        List<RaceResponse.LapSummary> laps = lapRepository
                .findByRaceIdOrderByKartNumberAscLapNumberAsc(race.getId())
                .stream()
                .map(lap -> new RaceResponse.LapSummary(
                        lap.getParticipant().getKartNumber(),
                        lap.getLapNumber(),
                        lap.getStartedAt(),
                        lap.getFinishedAt(),
                        lap.getDurationMs(),
                        lap.isPending()
                ))
                .toList();

        return new RaceResponse(
                race.getId(),
                race.getStatus(),
                race.getConfiguredLaps(),
                race.getStartedAt(),
                race.getFinishedAt(),
                kartNumbers,
                laps
        );
    }
}
