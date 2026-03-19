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

