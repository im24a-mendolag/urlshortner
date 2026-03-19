# URL Shortener Frontend

React + Vite frontend for the URL shortener project.

## Run locally

```bash
npm install
npm run dev
```

The app runs on `http://localhost:5173` by default.

## Required backend

Start the Spring Boot backend separately (default `http://localhost:8080`).

## Environment variables

Create `.env.local` (optional) to override defaults:

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_SHORT_BASE_URL=http://localhost:5173
VITE_SHORTEN_ENDPOINT=/shorten
VITE_DASHBOARD_ENDPOINT=/dashboard
VITE_STATS_BASE_ENDPOINT=/stats
VITE_LINKS_BASE_ENDPOINT=/links
```

Notes:
- If `VITE_API_BASE_URL` is omitted, frontend requests use relative paths.
- `VITE_SHORT_BASE_URL` controls the short-link base shown in UI.
- In dev, set `VITE_SHORT_BASE_URL` to the Vite host/port so unknown or disabled links show frontend UI.
- Endpoint vars let you adapt quickly if backend route names change.

## Main routes

- `/` public shortener page
- `/login` and `/register` for auth
- `/dashboard` authenticated user links + stats
- `/:code` short-code redirect route

## Dashboard actions

For logged-in users, dashboard supports:

- create short links
- view click stats
- disable/enable own links
