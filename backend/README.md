# URL Shortener Backend (Spring Boot)

Spring Boot backend for the URL shortener project.

## What this backend does

- Authentication endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/authenticate`
  - `POST /api/v1/auth/logout`
- Link endpoints:
  - `POST /api/v1/shorten`
  - `GET /api/v1/resolve/{code}`
  - `GET /{code}`
  - `GET /api/v1/dashboard`
  - `GET /api/v1/stats/{code}`
  - `PATCH /api/v1/links/{code}/disabled`

## Run locally

```powershell
.\mvnw.cmd spring-boot:run
```

Backend default URL: `http://localhost:8080`.

## Run tests

```powershell
.\mvnw.cmd test
```

## Configuration

Main config file:

- `src/main/resources/application.properties`

Important settings:

- `spring.jpa.hibernate.ddl-auto=update`
  - Keeps local data between restarts.
- Database URL/credentials and JWT settings can be provided via environment variables:
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET_KEY`, `JWT_EXPIRATION`, `JWT_COOKIE_NAME`, `JWT_COOKIE_MAX_AGE`

## Data model summary

- `users`
- `links`
- `link_clicks`
