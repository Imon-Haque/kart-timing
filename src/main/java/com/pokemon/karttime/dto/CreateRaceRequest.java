package com.pokemon.karttime.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRaceRequest(
        @NotNull @Min(1) @Max(100) Integer lapCount,
        @NotEmpty @Size(min = 1, max = 5) List<@NotBlank @Size(max = 32) String> kartNumbers
) {
}
