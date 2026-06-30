# URL Shortener

A production-shaped URL shortening service built with Spring Boot, PostgreSQL, and Redis.

Also ported to Python/FastAPI → [shortlink-api](https://github.com/FikratDev/shortlink-api), to check that the design decisions here (idempotent shortening, cache-aside Redis, atomic click counting) were sound choices and not just convenient ones in Spring.

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
| Infra | Docker Compose (local) · Terraform on AWS ECS Fargate + RDS (see `terraform/`) |
| CI/CD | GitHub Actions — test, build, push image to GHCR on every merge to main |

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

## Deploying to AWS

The `terraform/` directory provisions ECS Fargate, RDS Postgres, ECR, and the IAM roles needed to run this on AWS. See [`terraform/README.md`](terraform/README.md) for the full setup. CI builds and pushes the image on every push to `main`; `terraform apply` picks up the latest tag.

## Design notes

**Redis caching** — `resolve()` checks Redis before touching PostgreSQL. On a cache miss, the row is fetched from the DB and written back to Redis with a 24 h TTL. This keeps the redirect path fast without any schema changes.

**Idempotent shortening** — if the same URL is submitted twice, the existing code is returned instead of creating a duplicate row.

**Click tracking** — a single `UPDATE ... SET click_count = click_count + 1` runs per redirect. Both the cached and uncached paths increment the counter.

**Code generation** — 7-character substring of a random UUID (hex alphabet). Collision probability at 1 M URLs is negligible; the loop retries on the rare collision.
