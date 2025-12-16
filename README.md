# Scheduler Project — Boğaziçi University Course Scheduler

A full-stack web application that ingests Boğaziçi University course schedule pages, stores the parsed offerings in a PostgreSQL database, and provides a fast UI to search courses and build a weekly timetable.

This repository contains:
- **backend/** — Spring Boot REST API + data ingestion/parsing + PostgreSQL persistence
- **frontend/** — React (Vite) UI with schedule grid and selected-course management

---

## Why this project
University course scheduling is a classic real-world problem that combines:
- scraping / parsing imperfect HTML tables
- data modeling (terms, departments, offerings, meetings)
- scalable search endpoints
- a front-end schedule grid with add/remove and conflict visualization

This project is designed to be **portfolio-ready**: clean architecture, reproducible local setup, and a UI that demonstrates practical product thinking.

---

## Key features

### Backend
- **Term + Department sync**: Fetches HTML from the official schedule source and parses it into structured records.
- **Robust parsing**: Uses JSoup to parse table rows (including continuation rows such as LAB / P.S.).
- **Persistence**: Stores data in PostgreSQL via Spring Data JPA.
- **Search APIs**:
  - Search course offerings by code or name
  - Card endpoints optimized for UI listing
- **Admin sync endpoint**: Triggers bulk sync for a term (all departments).

### Frontend
- **Course search + list**: Filters by term / department and client-side query.
- **Add / remove** selected sections.
- **Schedule grid (Mon–Fri)**:
  - Renders meetings on the timetable
  - Highlights conflicts when multiple meetings land in the same day/time cell
- **No extra meeting fetch required**: The grid can be built directly from `Days/Hours/Rooms` strings returned by the card API.

---

## Tech stack

### Backend
- **Java + Spring Boot** (REST API)
- **Spring Data JPA / Hibernate** (ORM)
- **PostgreSQL** (primary database)
- **JSoup** (HTML parsing)
- **Maven Wrapper** (`mvnw.cmd`) for reproducible builds
- **Docker Compose** for local PostgreSQL

### Frontend
- **React**
- **Vite** (dev server + build tooling)
- **CSS** (component styles such as the schedule grid)

---

## High-level architecture

1. **Ingestion**
   - Backend fetches the schedule HTML for a given term + department.
   - Parser converts rows into `CourseOfferingDto` and `MeetingDto`.

2. **Persistence**
   - Entities such as `Term`, `Department`, `CourseOffering`, `Meeting` are stored using JPA.

3. **API layer**
   - Controllers provide endpoints for UI consumption (search + card views).

4. **Frontend**
   - UI calls the backend card/search endpoints.
   - Schedule grid is built by decoding `Days/Hours/Rooms` strings into meetings.

---

## Data model (conceptual)

- **Term**: e.g., `2025/2026-1`
- **Department**: e.g., `CMPE`
- **CourseOffering**: e.g., `CMPE150.01` (section-level offering, bound to a term + dept)
- **Meeting**: day/time/room records (lecture/ps/lab, if available)

---

## Running locally (Windows)

### Prerequisites
- **Java 17+** (or the version configured in your environment)
- **Node.js 18+**
- **Docker Desktop** (for PostgreSQL)

---

### 1) Start PostgreSQL
From the backend folder:

```bash
cd backend
docker compose up -d
```

Default DB config (see `backend/src/main/resources/application.properties`):
- DB: `boun_scheduler`
- User: `postgres`
- Password: `postgres`
- Port: `5432`

---

### 2) Run the backend (Spring Boot)

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

The backend exposes REST endpoints under `/api/*`.

---

### 3) Run the frontend (Vite)

```bash
cd frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal (commonly `http://localhost:5173`).

---

## Typical workflow

1. Start DB (Docker).
2. Start backend.
3. Start frontend.
4. In the UI:
   - Choose a term
   - Trigger admin sync (optional)
   - Search courses
   - Add courses to the timetable
   - Remove courses as needed

---

## API overview (selected)

These endpoints are referenced by the frontend code:

- `GET /api/courses/cards/all?term=...`
  - Returns a lightweight list for UI cards (including `daysText`, `hoursText`, `roomsText`).

- `GET /api/courses?term=...&q=...`
  - Searches offerings by code/name and returns offering views with meeting info.

- `POST /api/admin/sync/term?term=...`
  - Bulk sync for a term.

Note: exact request/response shapes may evolve—inspect controllers in `backend/src/main/java/com/furkan/scheduler/controller/`.

---

## Project structure

```text
scheduler/
  backend/   # Spring Boot API + ingestion + persistence
  frontend/  # React UI (Vite)
```

---

## Testing

Backend unit tests (if configured):

```bash
cd backend
.\mvnw.cmd test
```

Frontend linting/build (if configured):

```bash
cd frontend
npm run build
```

---

## Notes on reliability

HTML schedule pages can change over time. The ingestion layer is designed to be resilient by:
- locating the schedule table via header patterns
- supporting continuation rows (LAB / P.S.)
- normalizing whitespace and parsing days/slots/rooms defensively

---

## Future improvements (optional)

- Real-time conflict explanation (which courses conflict and why)
- Persist selected schedules to user profiles
- Export schedules (ICS/Calendar)
- Smarter slot handling (multi-slot meetings, variable durations)

---

## Author

**Furkan** — full-stack implementation (ingestion, backend API, and UI schedule builder).

If you’re reviewing this for a job application: I’m happy to walk through parsing strategy, data modeling decisions, and API/UI trade-offs in an interview.
