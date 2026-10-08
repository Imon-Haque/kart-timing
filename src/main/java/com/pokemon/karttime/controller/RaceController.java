package com.pokemon.karttime.controller;

import com.pokemon.karttime.service.RaceService;
import com.pokemon.karttime.dto.CreateRaceRequest;
import com.pokemon.karttime.dto.ImportResultResponse;
import com.pokemon.karttime.dto.PassingRequest;
import com.pokemon.karttime.dto.PassingResponse;
import com.pokemon.karttime.dto.RaceResponse;
import com.pokemon.karttime.dto.WinnerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/races")
@Tag(name = "Races")
@SecurityRequirement(name = "bearerAuth")
public class RaceController {

    private final RaceService raceService;

    public RaceController(RaceService raceService) {
        this.raceService = raceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new race (ADMIN)")
    public RaceResponse startRace(@Valid @RequestBody CreateRaceRequest request) {
        return raceService.startRace(request);
    }

    @PostMapping("/{id}/passings")
    @Operation(summary = "Record a start/finish passing (ADMIN)")
    public PassingResponse recordPassing(
            @PathVariable Long id,
            @Valid @RequestBody PassingRequest request
    ) {
        return raceService.recordPassing(id, request);
    }

    @PostMapping(value = "/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import passings from CSV (ADMIN)")
    public ImportResultResponse importCsv(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return raceService.importCsv(id, file);
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "Manually finish a race (ADMIN)")
    public RaceResponse finishRace(@PathVariable Long id) {
        return raceService.finishRace(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get race details")
    public RaceResponse getRace(@PathVariable Long id) {
        return raceService.getRaceResponse(id);
    }

    @GetMapping("/{id}/winner")
    @Operation(summary = "Get winner of a finished race")
    public WinnerResponse getWinner(@PathVariable Long id) {
        return raceService.getWinner(id);
    }

    @GetMapping("/latest/winner")
    @Operation(summary = "Get winner of the most recent race")
    public WinnerResponse getLatestWinner() {
        return raceService.getLatestWinner();
    }
}
