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

### C4. Auto booking reminders + email notifications
- **Problems fixed:** (a) `sendReminder` existed but nothing triggered it, so
  reminders never went out; (b) `spring-boot-starter-mail` was a dependency but
  no email was ever sent (WhatsApp only).
- **Reminders:** new `BookingReminderScheduler` (hourly) finds **CONFIRMED**
  bookings scheduled for **tomorrow** that haven't been reminded, publishes a new
  `BookingReminderEvent`, and sets a `reminder_sent` flag (at-most-once). The
  query uses `JOIN FETCH` so associations are safe to read in the async handler.
  Follows the existing event-driven pattern (booking stays decoupled from
  notifications).
- **Email:** new `EmailService` (Spring `JavaMailSender`) sends **confirmation**
  and **reminder** emails — asynchronous, best-effort (failures logged, customers
  without an email skipped). Wired into `BookingEventListener` alongside WhatsApp
  for both the confirmed and reminder events.
- **Robustness:** `approveBooking` initialises the customer/outlet associations
  in-transaction before publishing the confirmed event, so the async WhatsApp +
  email handlers don't hit lazy-loading errors.
- **Migration:** `V3__booking_reminder_flag.sql` (adds `bookings.reminder_sent`).

### C5. Payment module (completes the booking flow)
- **Problem fixed:** the documented flow is *slot lock → payment → confirm*, and
  the `payments` table + Razorpay config existed, but **no payment code existed**,
  so a booking could never be completed by the customer.
- **What was added** (`module/payment`, following the standard module layout):
  - `POST /api/v1/payments/initiate` — validates ownership + that the booking is
    `SLOT_LOCKED`, creates/reuses a `PENDING` payment and returns an order
    reference plus the public gateway key for checkout.
  - `POST /api/v1/payments/verify` — verifies the Razorpay HMAC-SHA256 signature
    (skipped when no key-secret is configured, so the flow is testable in dev),
    marks the payment `SUCCESS`, moves the booking to **CONFIRMED**, and publishes
    `BookingConfirmedEvent` (→ WhatsApp + email). Idempotent if already paid.
  - `POST /api/v1/payments/webhook` — public endpoint (already allow-listed in
    `SecurityConfig`) that marks payment success out-of-band.
  - `GET /api/v1/payments/booking/{bookingId}` — payment status for a booking.
- **Files:** `payment/{entity,repository,dto,service,controller}`.

### C6. Reviews & favourite stylists
- **Problem fixed:** both features were advertised and had tables, but no code.
- **Reviews** (`module/review`): `POST /api/v1/reviews` (customer can review only
  their **own COMPLETED** booking, one review per booking),
  `GET /api/v1/reviews/staff/{staffId}` (public) and `GET /api/v1/reviews/my`.
- **Favourites** (`module/favourite`): `POST`/`DELETE /api/v1/favourites/{staffId}`
  and `GET /api/v1/favourites`, using a composite-key (`@IdClass`) entity that
  matches the `favorite_staff` table; add is idempotent.

### C7. Real Admin & Staff frontend
- **Problem fixed:** only customer pages existed; the admin/staff screens were
  placeholders (and two menu links had no route at all), so owner/staff couldn't
  use the app.
- **Admin pages (real, API-driven):** Dashboard (revenue/bookings/outlet KPIs),
  **Bookings** (approve / reject / complete, paginated), **Staff** (list, register
  modal, live status change), **Services & Packages** (tabs, create/delete),
  **Outlets** (CRUD **including business hours** — the UI for feature C2),
  **Analytics** (revenue by outlet + popular services), **Notifications**
  (WhatsApp broadcast to selected numbers + campaign to all customers), **Gallery**
  (before/after grid with delete).
- **Staff pages (real):** Dashboard (present-days, today's count, **status
  switch**, check-in/out, today's assigned bookings with *Complete*) and a
  dedicated **Attendance** page (mark attendance + full history).
- **Supporting backend additions required by these screens** (found by
  mentally walking the UI as each role):
  - `GET /api/v1/staff/me` — a staff member had **no way** to load their own
    profile (needed for status/attendance/bookings, since every other endpoint
    needs a `staffId` they don't know).
  - `GET /api/v1/bookings/assigned` — staff-scoped booking list (the admin list is
    ADMIN-only and not filterable by staff).
  - `GET /api/v1/service-categories` — the admin "add service" form needs a
    category dropdown; there was no way to list categories.
- **Frontend plumbing:** new `paymentApi`, `reviewApi`, `favouriteApi` RTK Query
  slices (registered in the store), new route constants, `getCategories`,
  `getMyStaffProfile`, `getAssignedBookings` hooks, and routes for
  `/admin/outlets` + `/staff/attendance`.

### C8. Re-applied the two PR #1 bug fixes on this branch
While cross-verifying, the attendance crash and the `booking_ref` collision fix
were found **missing on this branch** (they were committed only on
`fix/staff-attendance-and-booking-ref`, and this branch was cut from the default
branch). Both are now applied here so the branch is self-consistent:
`StaffController` resolves phone → userId, and `booking_ref` uses the
collision-safe random generator with `existsByBookingRef`.

### C9. Frontend production white screen (Redux store ↔ Axios cycle)
- **Symptom:** the production bundle crashed before React mounted with
  `Uncaught ReferenceError: Cannot access 'Ol' before initialization`, leaving a
  completely white screen.
- **Root cause:** a runtime circular dependency was introduced by the auth API
  chain: `store.ts → authApi.ts → axiosBaseQuery.ts → axiosInstance.ts → store.ts`.
  The minified production bundle evaluated the imported store binding before
  `configureStore(...)` initialized it. Development/module ordering could hide
  this temporal-dead-zone failure, so changing import order would not be a safe fix.
- **Fix:** `axiosInstance.ts` no longer imports the store singleton. It exports
  `setupAxiosInterceptors(storeLike)` and receives only the minimal
  `getState`/`dispatch` contract it needs. `store.ts` creates the Redux store
  first, then installs the interceptors. Installation is idempotent so repeated
  setup/HMR does not register duplicate handlers.
- **Preserved behaviour:** Bearer token injection, the single-flight refresh
  guard, queuing/retrying concurrent 401 requests, `setTokens`, failed-refresh
  logout, and `/login` redirect are unchanged.
- **Import hygiene:** every component that needs only `RootState` now uses
  `import type`; `main.tsx` remains the only external runtime consumer of the
  store singleton.
- **Files:** `frontend/src/services/axiosInstance.ts`,
  `frontend/src/app/store.ts`, `frontend/src/App.tsx`, the four layout
  components, and `pages/customer/CustomerDashboard.tsx`.

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
| Fix C9 | `frontend/src/services/axiosInstance.ts` | injected, idempotent interceptor setup; no store import |
| Fix C9 | `frontend/src/app/store.ts` | install Axios interceptors after store creation |
| Fix C9 | `frontend/src/{App,components/layout/*,pages/customer/CustomerDashboard}.tsx` | type-only `RootState` imports |

---

## Known limitations / recommended follow-ups
- Duration conflicts are checked server-side, but simultaneous requests with
  different start times are not fully serialized because Redis locks are still
  keyed by exact start slot. A staff+date lock or DB exclusion constraint is the
  recommended hardening.
- The frontend Docker image intentionally runs `vite build` rather than the
  stricter `tsc && vite build`; keep an explicit compatible TypeScript check in CI.
- Razorpay signature verification is skipped only when no key secret is
  configured for local development. A real deployment must always provide the
  secret.
- A live Docker/browser run could not be performed in the authoring sandbox
  (no Docker daemon and frontend packages unavailable). The cycle removal and
  auth paths were statically cross-verified; run the exact browser checks in
  `HANDOFF.md` before merging.
