package dev.fikrat.urlshortener.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenRequest(
    @NotBlank(message = "URL must not be blank")
    @Pattern(regexp = "https?://.+", message = "Must be a valid HTTP or HTTPS URL")
    String url,

    @Nullable
    @Pattern(regexp = "[a-zA-Z0-9_-]{3,20}",
             message = "Custom code must be 3-20 characters: letters, digits, hyphens, underscores")
    String customCode
) {}
