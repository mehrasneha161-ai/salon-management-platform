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

## Part C — Features

### C1. Reschedule a booking (customer)
- **What:** customers can now move an existing booking to a new date/time (and
  optionally a different stylist) instead of only cancelling.
- **Endpoint:** `PUT /api/v1/bookings/{id}/reschedule` (role `CUSTOMER`), body
  `{ scheduledDate, scheduledTime, staffId? }`.
- **Behaviour (matches the create/cancel patterns):**
  - Ownership + status guard: only the owning customer, and not for
    `COMPLETED / CANCELLED / REJECTED` bookings.
  - **Locks the NEW slot first** (Redis `tryLock`); if it's taken →
    `409 SlotAlreadyLocked` and the old slot is left untouched.
  - On success, **releases the OLD slot lock**, updates date/time/stylist, and
    resets status to `SLOT_LOCKED` so it re-enters the admin-confirmation
    pipeline. Broadcasts slot updates for both the old and new dates.
  - Rescheduling to the exact same slot is rejected with a clear message.
- **Frontend:** a **Reschedule** button next to Cancel in *My Bookings*, opening
  a modal (DatePicker + 30-min TimePicker; past/today disabled to satisfy the
  `@Future` rule).
- **Files:** `booking/dto/request/RescheduleBookingRequest.java` (new),
  `booking/service/BookingService.java`, `booking/service/BookingServiceImpl.java`,
  `booking/controller/BookingController.java`,
  `frontend/src/features/booking/bookingApi.ts`,
  `frontend/src/pages/customer/BookingHistoryPage.tsx`.
- **Known limitation:** like `createBooking`, conflict prevention relies on the
  Redis slot lock (not a DB conflict check), so a slot whose lock has expired but
  which holds a confirmed booking is not re-validated — consistent with the
  existing create flow.

### C2. Configurable working hours + staff leave
- **Problem fixed:** working hours were hardcoded (09:00–20:00) in
  `SlotAvailabilityService`, there was no per-outlet/per-staff timing, and no
  leave concept — so a staff member on holiday still showed available slots.
- **What was added:**
  - **Outlet business hours** — `opening_time` / `closing_time` on `outlets`
    (default 09:00–20:00), settable via the outlet create/update API.
  - **Per-staff shift** — optional `shift_start` / `shift_end` on
    `staff_profiles` (NULL = follow outlet hours), settable via
    `PUT /api/v1/staff/{id}/shift`.
  - **Staff leave** — new `staff_leaves` table + endpoints
    `POST/GET /api/v1/staff/{id}/leave` and `DELETE /api/v1/staff/leave/{leaveId}`.
  - **Slot engine** now: returns **no slots** if the staff is on leave that day;
    otherwise builds the grid from the **effective window = outlet hours
    narrowed by the staff shift** (and returns empty for an invalid/empty window).
- **Migration:** `V2__working_hours_and_leaves.sql` (adds the columns + the
  `staff_leaves` table).
- **Files:** see the change table below.

### C3. Duration-aware slots (no more overlapping bookings)
- **Problem fixed:** the slot logic only removed each booking's exact **start**
  time, ignoring its duration — so a 60-min booking at 09:00 left 09:30 bookable,
  and `createBooking` (guarded only by the exact-slot Redis lock) let an
  overlapping booking at a *different* start time go through.
- **What was added:**
  - `getAvailableSlots` now treats each existing booking as an interval
    `[start, start+duration)` and only offers a start slot if the **whole
    requested service** `[start, start+durationMinutes)` fits before closing and
    overlaps nothing (so 09:30 is correctly blocked behind a 09:00×60 booking,
    and starts too late to finish are dropped).
  - New `SlotAvailabilityService.hasConflict(staffId, date, start, duration,
    excludeBookingId)` used by **`createBooking`** and **`rescheduleBooking`** to
    reject overlapping bookings server-side (409), not just hide them in the picker.
- **Known limitation:** the check is duration-aware but, like the rest of the
  flow, isn't fully serialized against simultaneous different-start requests
  (the Redis lock is per exact slot). A DB exclusion constraint or a staff+date
  lock would close that last race — noted for a future hardening PR.
- **Files:** `V2__...sql`, `outlet/entity/Outlet.java`,
  `staff/entity/StaffProfile.java`, `staff/entity/StaffLeave.java` (new),
  `staff/repository/StaffLeaveRepository.java` (new),
  `booking/service/SlotAvailabilityService.java`,
  `staff/service/StaffService(Impl).java`, `staff/controller/StaffController.java`,
  new DTOs `UpdateShiftRequest`, `LeaveRequest`, `LeaveResponse`,
  `staff/dto/response/StaffResponse.java`, and the outlet request/response +
  `OutletServiceImpl`.

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
| Feature C1 | `booking/dto/request/RescheduleBookingRequest.java` | **new** DTO |
| Feature C1 | `booking/service/BookingService(Impl).java` | `rescheduleBooking(...)` |
| Feature C1 | `booking/controller/BookingController.java` | `PUT /bookings/{id}/reschedule` |
| Feature C1 | `frontend/.../booking/bookingApi.ts` | `rescheduleBooking` mutation |
| Feature C1 | `frontend/.../customer/BookingHistoryPage.tsx` | Reschedule button + modal |

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
