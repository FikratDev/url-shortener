package dev.fikrat.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenRequest(
    @NotBlank(message = "URL must not be blank")
    @Pattern(regexp = "https?://.+", message = "Must be a valid HTTP or HTTPS URL")
    String url
) {}
