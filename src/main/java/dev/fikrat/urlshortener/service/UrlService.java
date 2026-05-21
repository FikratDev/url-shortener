package dev.fikrat.urlshortener.service;

import dev.fikrat.urlshortener.dto.ShortenResponse;
import dev.fikrat.urlshortener.dto.StatsResponse;
import dev.fikrat.urlshortener.exception.CodeNotFoundException;
import dev.fikrat.urlshortener.model.ShortUrl;
import dev.fikrat.urlshortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class UrlService {

    private static final int CODE_LENGTH = 7;
    private static final String CACHE_PREFIX = "url:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final UrlRepository repository;
    private final StringRedisTemplate redis;

    @Value("${app.base-url}")
    private String baseUrl;

    public UrlService(UrlRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
    }

    public ShortenResponse shorten(String originalUrl) {
        return repository.findByOriginalUrl(originalUrl)
            .map(this::toResponse)
            .orElseGet(() -> {
                String code = generateUniqueCode();
                ShortUrl saved = repository.save(new ShortUrl(code, originalUrl));
                redis.opsForValue().set(CACHE_PREFIX + code, originalUrl, CACHE_TTL);
                return toResponse(saved);
            });
    }

    public String resolve(String code) {
        String cached = redis.opsForValue().get(CACHE_PREFIX + code);
        if (cached != null) {
            repository.incrementClickCount(code);
            return cached;
        }
        ShortUrl url = repository.findByCode(code)
            .orElseThrow(() -> new CodeNotFoundException(code));
        repository.incrementClickCount(code);
        redis.opsForValue().set(CACHE_PREFIX + code, url.getOriginalUrl(), CACHE_TTL);
        return url.getOriginalUrl();
    }

    public StatsResponse stats(String code) {
        ShortUrl url = repository.findByCode(code)
            .orElseThrow(() -> new CodeNotFoundException(code));
        return new StatsResponse(url.getCode(), url.getOriginalUrl(), url.getClickCount(), url.getCreatedAt());
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, CODE_LENGTH);
        } while (repository.findByCode(code).isPresent());
        return code;
    }

    private ShortenResponse toResponse(ShortUrl url) {
        return new ShortenResponse(baseUrl + "/" + url.getCode(), url.getCode(), url.getOriginalUrl());
    }
}
