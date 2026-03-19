# URL Shortener Backend (Spring Boot)

Spring Boot backend for the URL shortener project.

## What this backend does

- Authentication endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/authenticate`
  - `POST /api/v1/auth/logout`
- Link endpoints:
  - `POST /shorten`
  - `GET /resolve/{code}`
  - `GET /{code}`
  - `GET /dashboard`
  - `GET /stats/{code}`
  - `PATCH /links/{code}/disabled`

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
- Database URL/credentials are set in that file.

## Data model summary

- `users`
- `links`
- `link_clicks`
