# URL Shortener Monorepo

This repository contains a complete URL shortener app:

- `backend/` - Spring Boot API (auth, shorten, redirect, stats, dashboard)
- `frontend/` - React + Vite UI

## Features

- User auth (`register`, `authenticate`, `logout`)
- Anonymous and authenticated URL shortening
- Redirect by short code with click tracking
- Dashboard + stats for logged-in users
- Owner-only link toggle (`active` / `disabled`)

## Prerequisites

- Java 25 (as configured in `backend/pom.xml`)
- Node.js + npm
- Docker (optional, for one-command setup)

## Quick start (Docker)

1) Create a local `.env` file (repo root) based on `.env.example` and fill in your credentials/secrets:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_KEY` (must be Base64)

2) Start everything:

```powershell
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## Run Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend runs on `http://localhost:8080`.

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

## Run Frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Frontend runs on Vite dev server (usually `http://localhost:5173`, or next free port).

## API overview

- **Auth (public)**:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/authenticate`
  - `POST /api/v1/auth/logout`
- **Links**:
  - `POST /api/v1/shorten` (public)
  - `GET /api/v1/resolve/{code}` (public, returns JSON)
  - `GET /{code}` (public, HTTP redirect)
  - `GET /api/v1/dashboard` (auth required)
  - `GET /api/v1/stats/{code}` (auth + owner required)
  - `PATCH /api/v1/links/{code}/disabled` (auth + owner required)

