# Phonebook Application — Java Port

A contact management application. This project is a port of a working Vue 3 / PostgreSQL / Playwright phonebook to a **Java 21 / Spring Boot** backend. The frontend, REST contract, database model, and end-to-end tests are preserved. The original FastAPI/SQLAlchemy backend is replaced with Spring MVC, Spring Data JPA, and Hibernate.

## What it does

The app is a phonebook / contact workspace:

- Create, view, edit, and delete contacts
- Name, phone number, email, address, tags, favorite flag, and timestamps
- Favorites and recently viewed lists
- Tags that can be shared across contacts (search and filter by tag)
- Universal search across name, phone number, and tag
- Filters: favorite, tag, unlabeled, recently viewed, search
- Sorting: name A–Z / Z–A, recently viewed, recently added, recently updated
- Pagination with page numbers, next/previous, and a page input
- CSV / XLSX / XLS import with preview, validation, duplicate detection, tags, and unrelated-column ignoring
- CSV export including tags
- Light / dark / system theme
- Responsive desktop and mobile layout

The sidebar is intentionally limited to **All Contacts**, **Favorites**, **Recently viewed**, and **Settings**. Tags are **not** shown in the sidebar; they are managed on contacts and in the tag filter.

## Architecture

```text
Browser  →  Vue 3 (localhost:5173)
                │  REST via Vite `/api` proxy
                ▼
         Spring Boot (localhost:8000)
                │  JPA / Hibernate
                ▼
         PostgreSQL (localhost:5432)
```

During Playwright:

```text
Playwright  →  Vite :5178  →  /api proxy  →  Spring Boot :8000  →  PostgreSQL
```

The Vue app always calls `/api`. Vite strips that prefix and forwards to the Java backend, so the frontend does not depend on whether the backend is Python or Java.

## Technologies

| Layer | Stack |
| --- | --- |
| Frontend | Vue 3, Vite, Pinia, Vue Router, Axios |
| Backend | Java 21, Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Jakarta Validation |
| Database | PostgreSQL 16 |
| Tests | Playwright |
| Containers | Docker, Docker Compose |

## Project structure

```text
PhoneBookAppli-Java/
├── backend/                 Spring Boot API (Java 21)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── mvnw / mvnw.cmd     Maven wrapper (uses local Maven or Docker)
│   └── src/main/java/com/phonebook/
├── frontend/               Existing Vue 3 UI
├── playwright/             Existing Playwright suite
├── docker-compose.yml
├── .env.example
└── README.md
```

## Prerequisites

- Docker Desktop (recommended: starts PostgreSQL, Java backend, and frontend together)
- Node.js 22+ for local frontend / Playwright
- Optional for host-side Java development: JDK 21 and Maven 3.9+

This machine may already have another process on port **8000**. The Phonebook Java API **must** own `localhost:8000`. If `GET http://localhost:8000/` does not return `{"message":"Phonebook API is running"}`, stop the other process first.

## Environment variables

Copy `.env.example` to `.env` (already gitignored):

```bash
POSTGRES_DB=phonebook
POSTGRES_USER=phonebook
POSTGRES_PASSWORD=change-me
POSTGRES_PORT=5432
```

| Variable | Purpose |
| --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL and Spring datasource credentials |
| `POSTGRES_PORT` | Host port mapped to PostgreSQL (`5432` inside Compose). Change this if host port 5432 is already taken |
| `DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME` / `DATABASE_USER` / `DATABASE_PASSWORD` | Used when running Spring Boot on the host |

Docker Compose sets `DATABASE_HOST=db` for the backend container. The Vite proxy target inside Compose is `http://backend:8000`.

## Docker setup

From the project root:

```bash
docker compose up --build
```

Open:

- Frontend: http://localhost:5173
- API: http://localhost:8000
- API root: http://localhost:8000/

Stop with `Ctrl+C`, or `docker compose down`. Data is stored in the `postgres_data` volume.

If host port 5432 is already in use, set `POSTGRES_PORT=5435` (or another free port) in `.env`. The backend container still talks to Postgres on the internal port 5432.

## Local development

### PostgreSQL

Either use Compose for the database only:

```bash
docker compose up db
```

or run PostgreSQL locally with the credentials from `.env`.

### Backend

```bash
cd backend
mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

If Maven is not installed, `mvnw` runs Maven inside a `maven:3.9-eclipse-temurin-21` Docker image. The API listens on port **8000**.

Package without running:

```bash
cd backend
mvnw.cmd clean package
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend listens on port **5173** and proxies `/api` to `http://localhost:8000`.

## Playwright

The suite starts (or reuses) the Java API on port 8000 and Vite on port 5178. If the API is not already the Phonebook API, Playwright runs `docker compose up --build db backend`.

```bash
cd playwright
npm install
npx playwright install chromium
npx playwright test --reporter=list
```

The suite covers CRUD, validation, search, phone search, tag search, combined filters, pagination, import, export, favorites, recently viewed, appearance, responsive layout, and the 1000-contact dataset test.

Playwright helpers call `http://localhost:8000` directly (not the Vite proxy). The UI tests go through Vite `:5178` so they exercise the same `/api` proxy as local development.

If port 8000 is occupied by a different application, tests fail with a clear message instead of treating that process as the Phonebook API.

## API overview

All JSON fields use `snake_case`. Errors use `{"detail": "..."}`.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Health message |
| `GET` | `/contacts/` | List contacts (`search`, `favorite`, `tag`, `unlabeled`, `recent`, `sort`, `page`, `limit`) |
| `POST` | `/contacts/` | Create contact |
| `GET` | `/contacts/{id}` | Get contact |
| `PUT` | `/contacts/{id}` | Update contact |
| `DELETE` | `/contacts/{id}` | Delete contact |
| `PATCH` | `/contacts/{id}/favorite` | Body: `{"is_favorite": true\|false}` |
| `PATCH` | `/contacts/{id}/viewed` | Mark recently viewed |
| `GET` | `/contacts/tags` | Tag list |
| `GET` | `/contacts/metrics` | Totals used by the dashboard cards |
| `POST` | `/contacts/import` | Body: `{"rows":[...]}` |

List `sort` values: `name_asc`, `name_desc`, `recently_viewed`, `recently_added`, `recently_updated`.

Duplicate phone numbers or emails return HTTP 400 with `detail: Phone number or email already exists`.

## Features

- Contact CRUD with tags
- Favorites filter and star toggle
- Recently viewed (timestamp updated when a contact is opened)
- Search by name, phone, or tag without a full page reload
- Tag filter combined with search
- Untagged filter
- Configurable page size in Settings
- Import CSV/XLSX/XLS via file picker or drag-and-drop
- Export of the current filtered contact set as `phonebook-contacts.csv`

## Import / export

Import is parsed in the browser (CSV or Excel). Only `name`, `phone_number`, `email`, `address`, and `tags` columns are used; other columns are ignored. The frontend validates rows, then `POST /contacts/import` inserts valid contacts and reports skipped duplicates.

Export downloads a UTF-8 CSV (with BOM) of the contacts matching the current search/filters, including a `tags` column.

## Database

Tables:

- `contacts` — `id`, `name`, `phone_number` (unique), `email` (unique, nullable), `address`, `is_favorite`, `last_viewed_at`, `created_at`, `updated_at`
- `tags` — `id`, `name` (unique)
- `contact_tags` — many-to-many join

Hibernate `ddl-auto=update` creates or updates this schema on startup. Hibernate messages such as `constraint "uk_contacts_phone_number" does not exist, skipping` appear when it tries to drop an old constraint name before creating the current one. On a fresh database they are harmless. Do not point this backend at an unrelated database on port 5432.

## Troubleshooting

**`Unable to save contact` / Playwright modal stays open**  
Confirm `GET http://localhost:8000/` returns `{"message":"Phonebook API is running"}`. A different process on port 8000 (for example another FastAPI app) will make Vite proxy create/list/metrics calls fail.

**Vite `/api` returns 502**  
The Java backend is not running on port 8000.

**Docker Compose fails on port 5432**  
Another Postgres is bound to 5432. Set `POSTGRES_PORT` in `.env` to a free port.

**Java 17 / no Maven on the host**  
Use Docker Compose for the API, or `backend/mvnw.cmd` which can run Maven in Docker.

**Playwright cannot start Chromium**  
`cd playwright && npx playwright install chromium`

## Java port notes

This repository is a port, not a redesign:

- The Vue UI, routes, and Playwright specs are the behavioral specification
- Tags stay off the sidebar
- JSON field names, status codes, and error `{detail}` bodies match the original API
- Import still happens in the frontend; the Java API receives already-parsed rows
- Playwright still drives Vite rather than calling Spring from the browser directly
