# URL Shortener

A production-shaped URL shortening service built with Spring Boot, PostgreSQL, and Redis.

## What it does

- `POST /api/shorten` — shorten any HTTP/HTTPS URL, returns a 7-character code
- `GET /{code}` — redirects to the original URL (HTTP 302), increments click count
- `GET /api/stats/{code}` — returns click count, original URL, and creation time
- Submitting the same URL twice returns the same code — no duplicates
- Hot redirects served from Redis (24 h TTL); cold misses fall back to PostgreSQL

## Stack

| Layer | Tech |
|---|---|
| API | Java 21, Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Docs | Swagger UI (`/swagger-ui.html`) |
| Infra | Docker Compose |

## Quick start

```bash
docker compose up --build
```

API at `http://localhost:8080` · Swagger at `http://localhost:8080/swagger-ui.html`

## API

### Shorten a URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/some/very/long/path"}'
```
```json
{
  "shortUrl": "http://localhost:8080/a3f9c12",
  "code": "a3f9c12",
  "originalUrl": "https://example.com/some/very/long/path"
}
```

### Redirect
```bash
curl -L http://localhost:8080/a3f9c12
# → 302 redirect to original URL
```

### Stats
```bash
curl http://localhost:8080/api/stats/a3f9c12
```
```json
{
  "code": "a3f9c12",
  "originalUrl": "https://example.com/some/very/long/path",
  "clickCount": 4,
  "createdAt": "2026-05-21T10:00:00Z"
}
```

## Running tests

```bash
mvn test
```

## Design notes

**Redis caching** — `resolve()` checks Redis before touching PostgreSQL. On a cache miss, the row is fetched from the DB and written back to Redis with a 24 h TTL. This keeps the redirect path fast without any schema changes.

**Idempotent shortening** — if the same URL is submitted twice, the existing code is returned instead of creating a duplicate row.

**Click tracking** — a single `UPDATE ... SET click_count = click_count + 1` runs per redirect. Both the cached and uncached paths increment the counter.

**Code generation** — 7-character substring of a random UUID (hex alphabet). Collision probability at 1 M URLs is negligible; the loop retries on the rare collision.
