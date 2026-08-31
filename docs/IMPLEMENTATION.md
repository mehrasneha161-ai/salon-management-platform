# Implementation Notes

This document lists **what was built / changed** in the Salon Management Platform
across two pieces of work:

1. **Bug fixes** (branch `fix/staff-attendance-and-booking-ref`, PR #1)
2. **Full dockerisation** (branch `chore/dockerise-full-stack`, PR #2)

> For *how* each change was made and *how to test it locally*, see
> [`HANDOFF.md`](./HANDOFF.md).

---

## Part A — Bug fixes

### A1. Staff attendance was completely broken (HTTP 500 on every call)
- **Symptom:** every `POST /api/v1/staff/attendance/check-in` and `/check-out`
  returned `500 Internal Server Error`. Staff could never mark attendance.
- **Root cause:** `StaffController` did
  `staffService.checkIn(UUID.fromString(userDetails.getUsername()))`, but the
  authenticated username is the **phone number** (set in
  `CustomUserDetailsService`), not a UUID. `UUID.fromString("9999999999")`
  throws `IllegalArgumentException` → falls through to the generic handler → 500.
- **Fix:** resolve phone → user id via `UserRepository`, mirroring the existing
  `BookingController.resolveUserId()` pattern. Added a `UserRepository`
  dependency + a private `resolveUserId(UserDetails)` helper, and used it in
  both check-in and check-out.
- **Files:** `backend/.../staff/controller/StaffController.java`
- **Note:** this same fix is also applied on the `chore/dockerise-full-stack`
  branch, so that branch is self-contained (attendance works after a plain
  `docker compose up`, even before PR #1 is merged).

### A2. Duplicate `booking_ref` (unique-constraint violation)
- **Symptom:** after an app restart (or with more than one instance), creating a
  booking could fail with a `booking_ref` unique-constraint violation.
- **Root cause:** the reference used an in-memory `static AtomicInteger` that
  resets to `1` on every restart and is not shared across instances, so it
  regenerated already-used values like `BK-20260831-0001`.
- **Fix:** generate `BK-<yyyyMMdd>-<random 6-char>` (18 chars, fits
  `VARCHAR(20)`), verified against a new
  `BookingRepository.existsByBookingRef(...)` with a short retry loop; throws a
  clean `BusinessException` in the astronomically unlikely case of repeated
  collisions. Restart- and multi-instance-safe.
- **Files:** `backend/.../booking/service/BookingServiceImpl.java`,
  `backend/.../booking/repository/BookingRepository.java`

---

## Part B — Full dockerisation (`docker compose up` runs everything)

The goal was: **one command (`docker compose up -d --build`) brings up the
entire stack.** Several things blocked a clean bring-up; each was fixed.

### B1. Frontend Docker image (was missing entirely)
- `docker-compose.yml` referenced `frontend/Dockerfile`, which did not exist —
  so the frontend service failed to build.
- **Added** `frontend/Dockerfile`: multi-stage — Node 20 builds the app with
  Vite, then the static output is served by nginx.

### B2. Frontend → backend routing (nginx reverse proxy)
- The SPA calls the API with **relative** URLs (`/api/v1/...`) and connects to
  WebSocket at `/ws`. Without a proxy, the browser would hit the frontend
  origin for `/api` and get 404s.
- **Added** `frontend/nginx.conf`:
  - SPA fallback (`try_files ... /index.html`) so React Router deep links work.
  - Reverse-proxy `/api/` → `backend:8080`.
  - Reverse-proxy `/ws` → `backend:8080` with WebSocket upgrade headers.
  - Sensible caching for hashed `/assets/` and no-cache for `index.html`.

### B3. Backend wouldn't start — Flyway migration was off the classpath
- `application.yml` uses `flyway.locations: classpath:db/migration`, but the SQL
  lived at `/database/V1__init_schema.sql` (not on the classpath). Flyway found
  no migrations, so with `ddl-auto: validate` the schema/tables never existed.
- **Fix:** moved the migration to
  `backend/src/main/resources/db/migration/V1__init_schema.sql` so it is
  packaged into the jar and applied on startup (creates all tables + seeds the
  default admin and service categories).

### B4. Frontend wouldn't compile — 8 imported pages were missing
- `App.tsx` imports `pages/staff/StaffDashboard` and seven `pages/admin/*`
  screens that did not exist → the Vite build failed on unresolved imports.
- **Added** 8 clean Ant Design **placeholder** pages so the app compiles and
  every route renders. They are intentionally minimal (title + info card) and
  are ready to be wired to the already-existing feature APIs:
  - `pages/staff/StaffDashboard.tsx`
  - `pages/admin/AdminDashboard.tsx`
  - `pages/admin/AdminBookingsPage.tsx`
  - `pages/admin/AdminStaffPage.tsx`
  - `pages/admin/AdminServicesPage.tsx`
  - `pages/admin/AdminAnalyticsPage.tsx`
  - `pages/admin/AdminNotificationsPage.tsx`
  - `pages/admin/AdminGalleryPage.tsx`

### B5. Backend Docker image (hardened)
- **Rewrote** `backend/Dockerfile`:
  - Multi-stage Maven build with `dependency:go-offline` for dependency-layer
    caching (faster rebuilds).
  - Slim `eclipse-temurin:17-jre-alpine` runtime running as a **non-root** user.
  - Container-aware heap (`-XX:MaxRAMPercentage`) and **env-driven** Spring
    profile (removed the hardcoded `prod`).

### B6. `docker-compose.yml` hardening
- Health checks for **backend** (`/actuator/health`) and **frontend**.
- Ordered startup: `backend` waits for Postgres + Redis **healthy**; `frontend`
  waits for `backend` **healthy**.
- A one-shot **`minio-init`** service that auto-creates the `salon-assets`
  bucket and sets a public-read policy (uploads would otherwise fail).
- Corrected Spring Boot 3 Redis env keys (`SPRING_DATA_REDIS_*`).
- Removed the obsolete top-level `version` key.

### B7. Repo hygiene & build config
- **`.gitignore`** (root): ignores `.env`, `node_modules`, `target`, `dist`.
- **`.dockerignore`** for both `backend/` and `frontend/` (smaller build
  context, no secrets).
- **`frontend/postcss.config.js`** (Tailwind + Autoprefixer) and
  **`frontend/tsconfig.node.json`** (referenced by `tsconfig.json`) for a
  correct/complete frontend build.
- **`README.md`**: corrected clone URL, documented the one-command run and the
  seeded admin login.

---

## Full list of changed / added files

| Area | File | Change |
|---|---|---|
| Bug A1 | `backend/.../staff/controller/StaffController.java` | resolve phone→userId |
| Bug A2 | `backend/.../booking/service/BookingServiceImpl.java` | collision-safe ref |
| Bug A2 | `backend/.../booking/repository/BookingRepository.java` | `existsByBookingRef` |
| Docker | `frontend/Dockerfile` | **new** — Vite build → nginx |
| Docker | `frontend/nginx.conf` | **new** — SPA + `/api` + `/ws` proxy |
| Docker | `frontend/.dockerignore` | **new** |
| Docker | `backend/Dockerfile` | rewritten (cache, non-root, env profile) |
| Docker | `backend/.dockerignore` | **new** |
| Flyway | `backend/src/main/resources/db/migration/V1__init_schema.sql` | **moved** here |
| Build | `frontend/postcss.config.js`, `frontend/tsconfig.node.json` | **new** |
| Frontend | `frontend/src/pages/{staff,admin}/*.tsx` (8 files) | **new** scaffolds |
| Compose | `docker-compose.yml` | healthchecks, minio-init, deps, redis keys |
| Repo | `.gitignore` | **new** |
| Docs | `README.md` | run steps + admin login |

---

## Known limitations / recommended follow-ups
- The **admin/staff pages are placeholders** (so the build & routes work). Wire
  them to the existing APIs (bookings approve/reject, analytics charts, staff
  attendance, broadcasts) in a follow-up.
- The frontend image runs `vite build` (esbuild) directly rather than
  `tsc && vite build`, so a strict type-nit doesn't block the runtime image —
  keep type-checking in CI/lint.
- Pre-existing product gaps still open: **Payment module** and **Reviews /
  Favourites** modules are referenced (tables/config exist) but not implemented;
  booking reminders have no scheduler; slot availability ignores service
  duration. These are out of scope for these two PRs.
