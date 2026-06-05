package dev.fikrat.urlshortener.controller;

import dev.fikrat.urlshortener.dto.ShortenRequest;
import dev.fikrat.urlshortener.dto.ShortenResponse;
import dev.fikrat.urlshortener.dto.StatsResponse;
import dev.fikrat.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "URL Shortener")
public class UrlController {

    private final UrlService service;

    public UrlController(UrlService service) {
        this.service = service;
    }

    @PostMapping("/api/shorten")
    @Operation(summary = "Shorten a URL")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.ok(service.shorten(request.url(), request.customCode()));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Redirect to the original URL")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String originalUrl = service.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, originalUrl)
            .build();
    }

    @GetMapping("/api/stats/{code}")
    @Operation(summary = "Get click count and metadata for a short URL")
    public ResponseEntity<StatsResponse> stats(@PathVariable String code) {
        return ResponseEntity.ok(service.stats(code));
    }
}
