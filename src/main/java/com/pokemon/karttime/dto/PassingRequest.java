package com.pokemon.karttime.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public record PassingRequest(
        @NotBlank @Size(max = 32) String kartNumber,
        @NotNull @DateTimeFormat(pattern = "HH:mm:ss") LocalTime passingTime
) {
}
