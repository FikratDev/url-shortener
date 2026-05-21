package dev.fikrat.urlshortener.dto;

public record ShortenResponse(String shortUrl, String code, String originalUrl) {}
