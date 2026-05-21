package dev.fikrat.urlshortener.repository;

import dev.fikrat.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByCode(String code);
    Optional<ShortUrl> findByOriginalUrl(String originalUrl);

    @Modifying
    @Transactional
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.code = :code")
    void incrementClickCount(String code);
}
