package dev.fikrat.urlshortener.service;

import dev.fikrat.urlshortener.dto.ShortenResponse;
import dev.fikrat.urlshortener.exception.CodeNotFoundException;
import dev.fikrat.urlshortener.model.ShortUrl;
import dev.fikrat.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlServiceTest {

    @Mock private UrlRepository repository;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private UrlService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shorten_existingUrl_returnsExistingCode() {
        ShortUrl existing = new ShortUrl("abc1234", "https://example.com");
        when(repository.findByOriginalUrl("https://example.com")).thenReturn(Optional.of(existing));

        ShortenResponse response = service.shorten("https://example.com");

        assertThat(response.code()).isEqualTo("abc1234");
        verify(repository, never()).save(any());
    }

    @Test
    void shorten_newUrl_savesAndReturnsShortUrl() {
        when(repository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(repository.findByCode(anyString())).thenReturn(Optional.empty());
        ShortUrl saved = new ShortUrl("xyz7890", "https://new.com");
        when(repository.save(any())).thenReturn(saved);
        doNothing().when(valueOps).set(anyString(), anyString(), any());

        ShortenResponse response = service.shorten("https://new.com");

        assertThat(response.shortUrl()).contains("xyz7890");
        verify(repository).save(any(ShortUrl.class));
    }

    @Test
    void resolve_cachedCode_returnsFromCacheWithoutDbLookup() {
        when(valueOps.get("url:abc1234")).thenReturn("https://example.com");
        doNothing().when(repository).incrementClickCount("abc1234");

        String result = service.resolve("abc1234");

        assertThat(result).isEqualTo("https://example.com");
        verify(repository, never()).findByCode(any());
    }

    @Test
    void resolve_uncachedCode_hitsDbAndCaches() {
        when(valueOps.get("url:abc1234")).thenReturn(null);
        ShortUrl url = new ShortUrl("abc1234", "https://example.com");
        when(repository.findByCode("abc1234")).thenReturn(Optional.of(url));
        doNothing().when(repository).incrementClickCount("abc1234");
        doNothing().when(valueOps).set(anyString(), anyString(), any());

        String result = service.resolve("abc1234");

        assertThat(result).isEqualTo("https://example.com");
        verify(valueOps).set(eq("url:abc1234"), eq("https://example.com"), any());
    }

    @Test
    void resolve_unknownCode_throwsCodeNotFoundException() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(repository.findByCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("unknown"))
            .isInstanceOf(CodeNotFoundException.class)
            .hasMessageContaining("unknown");
    }
}
