# Handoff & Local Testing Guide

This is the practical companion to [`IMPLEMENTATION.md`](./IMPLEMENTATION.md).
It explains **how each change was made** and gives you **exact steps to run and
verify everything on your machine**.

---

## 1. Branches / PRs

| Work | Branch | PR |
|---|---|---|
| Bug fixes (attendance + booking ref) | `fix/staff-attendance-and-booking-ref` | #1 |
| Full dockerisation | `chore/dockerise-full-stack` | #2 |

> The **attendance fix (A1)** is included on **both** branches, so the
> dockerisation branch works end-to-end on its own (see §4b to verify).

To test the complete result, use the dockerisation branch (it is based on the
default branch; merge/checkout as needed):

```bash
git clone https://github.com/mehrasneha161-ai/salon-management-platform
cd salon-management-platform
git checkout chore/dockerise-full-stack
```

> If both PRs are merged into the default branch, just use the default branch.

---

## 2. How each change was implemented (summary of steps)

### Bug A1 — staff attendance 500
1. Confirmed the auth username is the **phone number** (`CustomUserDetailsService`
   sets `.username(user.getPhoneNumber())`; the JWT subject is the phone).
2. Confirmed `StaffService.checkIn(UUID userId)` expects the **user id**, and that
   `BookingController` already had the correct `resolveUserId()` helper.
3. Injected `UserRepository` into `StaffController`, added a private
   `resolveUserId(UserDetails)` that does
   `findByPhoneNumberAndIsDeletedFalse(username).orElseThrow().getId()`, and used
   it in `checkIn`/`checkOut`.

### Bug A2 — duplicate booking_ref
1. Removed the in-memory `AtomicInteger` counter.
2. Added `boolean existsByBookingRef(String)` (Spring Data derived query, checks
   all rows — including soft-deleted, since the DB unique constraint spans all).
3. Generate `BK-<yyyyMMdd>-<random 6 chars A–Z0–9>`, check `existsByBookingRef`,
   retry up to 5 times, else throw `BusinessException`.

### Feature C1 — Reschedule a booking
1. Added `RescheduleBookingRequest` DTO (`scheduledDate` `@Future`,
   `scheduledTime`, optional `staffId`, `sessionId`).
2. Added `rescheduleBooking(...)` to `BookingService`/`BookingServiceImpl`:
   ownership + status guard → lock the **new** slot → release the **old** slot →
   update date/time/stylist → status `SLOT_LOCKED` → broadcast both dates.
3. Added `PUT /api/v1/bookings/{id}/reschedule` in `BookingController`.
4. Frontend: `rescheduleBooking` RTK Query mutation + a Reschedule button and
   modal (DatePicker + TimePicker) in *My Bookings*.

### Feature C2 — Configurable working hours + staff leave
1. Migration `V2` adds `outlets.opening_time/closing_time`,
   `staff_profiles.shift_start/shift_end`, and the `staff_leaves` table.
2. Entities updated (`Outlet`, `StaffProfile`) + new `StaffLeave` +
   `StaffLeaveRepository`.
3. `SlotAvailabilityService.getAvailableSlots` now short-circuits to empty when
   the staff is on leave, and builds slots from the intersection of outlet hours
   and the staff shift.
4. New endpoints: `PUT /staff/{id}/shift`, `POST/GET /staff/{id}/leave`,
   `DELETE /staff/leave/{leaveId}`; outlet hours flow through the outlet
   create/update API.

### Feature C3 — Duration-aware slots (no overlaps)
1. `getAvailableSlots` builds busy intervals `[start, start+duration)` from
   existing bookings and only offers a start if the full requested service fits
   before closing and overlaps nothing.
2. Added `SlotAvailabilityService.hasConflict(...)`; `createBooking` and
   `rescheduleBooking` call it and reject overlaps (409) server-side.

### Feature C4 — Auto reminders + email
1. Migration `V3` adds `bookings.reminder_sent`.
2. `BookingReminderScheduler` (hourly, `@Scheduled`) publishes `BookingReminderEvent`
   for CONFIRMED bookings scheduled tomorrow and flips `reminder_sent` (at-most-once).
3. `BookingEventListener` now also handles the reminder event and, for BOTH
   confirmation and reminder, sends email via the new `EmailService`
   (`JavaMailSender`, async, best-effort). `approveBooking` pre-initialises
   associations so async handlers don't hit lazy-loading errors.

### Features C5–C8 — Payment, reviews, favourites, admin/staff UI
1. **Payment** (`module/payment`): entity/repo/DTOs/service/controller. `initiate`
   creates a PENDING payment for a `SLOT_LOCKED` booking; `verify` checks the
   Razorpay HMAC (skipped in dev when no key-secret) then marks payment SUCCESS,
   sets the booking CONFIRMED and publishes `BookingConfirmedEvent`; `webhook`
   does the same out-of-band.
2. **Reviews** (`module/review`) and **Favourites** (`module/favourite`) added with
   the same module layout; favourites uses an `@IdClass` composite key.
3. **Admin/Staff UI:** all placeholder pages replaced with real API-driven screens;
   added missing routes (`/admin/outlets`, `/staff/attendance`).
4. **Backend gaps found by walking the UI as each role** and then added:
   `GET /staff/me`, `GET /bookings/assigned`, `GET /service-categories`.
5. Re-applied the two PR #1 fixes (attendance crash, `booking_ref`) which were
   missing on this branch.

### Fix C9 — production white screen / circular module initialization
1. Traced the runtime cycle as
   `store.ts → authApi.ts → axiosBaseQuery.ts → axiosInstance.ts → store.ts`.
2. Removed the store singleton import from `axiosInstance.ts` and exported
   `setupAxiosInterceptors(storeLike)` with only the required auth state and
   dispatch contract.
3. `store.ts` now calls setup only after `configureStore(...)` returns. A
   module-level guard makes setup idempotent for duplicate calls/HMR.
4. Converted every `RootState`-only component import to `import type`, leaving
   `main.tsx` as the sole external runtime importer of the store.
5. Cross-verified unchanged auth behaviour: token injection; one refresh for
   concurrent 401s; queued retries with the new token; and queue rejection,
   logout, and `/login` redirect when refresh fails.

### Feature C10 — Coupons and discounts
1. Migration `V4__coupon_discount_engine.sql` adds constrained `coupons` and
   `coupon_redemptions` tables; immutable booking subtotal/discount/coupon
   snapshots and `slot_locked_at`; and payment reconciliation fields. Existing
   booking subtotal is backfilled from total with zero discount.
2. Added the full `module/coupon` architecture: entities → repositories (including
   pessimistic locks) → service interface/implementation → request/response DTOs
   → secured controller. Admin manages coupons; customers can validate only.
3. Validation uses trusted catalog price and is advisory. Booking creation sends
   the displayed coupon id and amounts; the backend recalculates under lock and
   rejects changed quotes before taking a slot lock.
4. Booking creation writes one immutable pricing snapshot and a `RESERVED`
   redemption. Confirmation/payment/completion makes it `REDEEMED`; unpaid
   cancellation, rejection, or 11-minute cleanup makes it `RELEASED`.
   Rescheduling does not extend `slot_locked_at` or reprice the coupon.
5. Percentage/fixed arithmetic uses two-decimal `BigDecimal` `HALF_UP`, checks
   minimum spend, optional cap/scope/date/limits, and clamps final total to zero.
   A zero-total booking confirms without payment.
6. Late valid settlement for a terminal booking is recorded as SUCCESS but does
   not revive the booking or touch the coupon; it is flagged for manual
   refund/reconciliation.
7. Frontend adds **Admin → Coupons**, RTK API/store/types/routes, and customer
   Apply/Remove coupon controls with a server quote and subtotal/discount/total.

### Dockerisation
1. **Frontend image:** Node build stage runs `npx vite build` → `dist`; nginx
   stage serves `dist` and uses `nginx.conf`.
2. **nginx.conf:** SPA fallback + proxy `/api/` and `/ws` to `backend:8080`
   (with `Upgrade`/`Connection` headers for WebSocket).
3. **Flyway:** moved `V1__init_schema.sql` into
   `backend/src/main/resources/db/migration/` (the classpath location the app
   already looks in).
4. **Missing pages:** created the 8 `admin/`+`staff/` page components that
   `App.tsx` imports, as Ant Design placeholders.
5. **Backend image:** multi-stage Maven (dep caching) → non-root JRE runtime.
6. **compose:** added healthchecks, `depends_on … condition`, a `minio-init`
   bucket-creation job, corrected `SPRING_DATA_REDIS_*` keys.
7. **Hygiene:** `.gitignore`, `.dockerignore` (x2), `postcss.config.js`,
   `tsconfig.node.json`.

---

## 3. Local testing — Option A: Docker (recommended)

### Prerequisites
- Docker Engine + **Docker Compose v2** (`docker compose version`).
- Free ports: `5173`, `8080`, `5432`, `6379`, `9000`, `9001`.

### Steps
```bash
# 1. Create your env file
cp .env.example .env
#    IMPORTANT: JWT_SECRET must be >= 32 characters (HS256). The example value
#    already satisfies the length; change all secrets before any real use.

# 2. Build & start the whole stack
docker compose up -d --build

# 3. Watch it come up (backend takes ~40–60s on first boot)
docker compose ps
docker compose logs -f backend      # Ctrl-C to stop tailing
```

### What "healthy" looks like
```bash
docker compose ps
# postgres    healthy
# redis       healthy
# minio       running
# minio-init  exited (0)      <-- one-shot bucket creator, exiting is correct
# backend     healthy
# frontend    healthy
```

### Verify each layer
```bash
# Backend health
curl http://localhost:8080/actuator/health          # {"status":"UP"}

# Flyway ran (schema applied)
docker compose logs backend | grep -i "flyway\|migrat"
#   ...Successfully applied 1 migration...

# MinIO bucket auto-created
docker compose logs minio-init                       # "minio bucket ready"
```

Open in the browser:
- **Frontend:** http://localhost:5173
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **MinIO console:** http://localhost:9001 (user/pass = `AWS_ACCESS_KEY` /
  `AWS_SECRET_KEY` from `.env`; default `minioadmin`/`minioadmin`) → confirm the
  `salon-assets` bucket exists.

**Login as the seeded admin:** phone `9999999999`, password `Admin@123`.

---

## 4. Verify the two bug fixes

You can do this from **Swagger UI** (easiest) or curl. Below is curl.

### 4a. Booking reference (Bug A2)
The default admin can be used to explore, but a booking needs a CUSTOMER. Quick
path — register a customer and create a booking:

```bash
BASE=http://localhost:8080/api/v1

# Register a customer (returns accessToken)
curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' -d '{
  "fullName":"Test Customer","phoneNumber":"9812300001","password":"Passw0rd!"
}'
```
Create a booking with that token (needs a real outlet/staff/service id — create
them as admin first, or use Swagger which lists ids). The response
`data.bookingRef` should look like **`BK-20260831-K3P9ZQ`**.

**Restart-safety check** (this is what the fix is about):
```bash
docker compose restart backend
# create another booking -> you get a NEW unique ref, no 500 / no unique violation.
```

### 4b. Staff attendance (Bug A1)
```bash
BASE=http://localhost:8080/api/v1

# 1. Login as admin -> grab accessToken
ADMIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"9999999999","password":"Admin@123"}' | \
  sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 2. Create an outlet (needed for the staff profile)
OUTLET=$(curl -s -X POST $BASE/outlets -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Downtown","address":"MG Road","city":"Pune"}' | \
  sed -E 's/.*"id":"([0-9a-f-]+)".*/\1/')

# 3. Register a staff member
curl -s -X POST $BASE/staff -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Sam Stylist\",\"phoneNumber\":\"9812311111\",\"password\":\"Passw0rd!\",\"outletId\":\"$OUTLET\",\"specialization\":\"Hair\"}"

# 4. Login as that staff -> token
STAFF=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"phoneNumber":"9812311111","password":"Passw0rd!"}' | \
  sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 5. Check in  --> BEFORE the fix this was HTTP 500; now expect 200 "Checked in"
curl -i -X POST $BASE/staff/attendance/check-in -H "Authorization: Bearer $STAFF"

# 6. Check out --> expect 200 "Checked out"
curl -i -X POST $BASE/staff/attendance/check-out -H "Authorization: Bearer $STAFF"
```
**Expected:** step 5 and 6 return `200` (they returned `500` before the fix).

---

### 4c. Reschedule a booking (Feature C1)
**UI:** log in as a customer → **My Bookings** → click **Reschedule** on a
PENDING/SLOT_LOCKED/CONFIRMED row → pick a new (future) date + time → submit.
The row moves to the new date/time and status returns to `SLOT_LOCKED`
(awaiting admin confirmation).

**API:**
```bash
BASE=http://localhost:8080/api/v1
# CUST = a customer accessToken; BOOKING_ID = an existing booking id of that customer
curl -i -X PUT $BASE/bookings/$BOOKING_ID/reschedule \
  -H "Authorization: Bearer $CUST" -H 'Content-Type: application/json' \
  -d '{"scheduledDate":"2026-12-01","scheduledTime":"14:30"}'
```
Expected: `200` with the updated booking (new date/time, status `SLOT_LOCKED`).
If the target slot is being booked by someone else → `409` `SLOT_ALREADY_LOCKED`.
Rescheduling to the exact same slot → `400` with a clear message.

### 4d. Working hours + leave (Feature C2)
Using an ADMIN token (`$ADMIN`) and an existing `$STAFF_ID` / `$OUTLET_ID`:
```bash
BASE=http://localhost:8080/api/v1

# Set outlet hours 10:00–18:00 (send with an outlet update)
# (include the outlet's required name/address fields in the body too)

# Set a staff shift 12:00–16:00
curl -i -X PUT $BASE/staff/$STAFF_ID/shift -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"shiftStart":"12:00","shiftEnd":"16:00"}'

# Available slots now only span the effective window (12:00–15:30)
curl -s "$BASE/slots/available?outletId=$OUTLET_ID&staffId=$STAFF_ID&date=2026-12-01&durationMinutes=30"

# Put the staff on leave for that day
curl -i -X POST $BASE/staff/$STAFF_ID/leave -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"startDate":"2026-12-01","endDate":"2026-12-01","reason":"Holiday"}'

# Slots for that date are now EMPTY (on leave) even if attendance exists
curl -s "$BASE/slots/available?outletId=$OUTLET_ID&staffId=$STAFF_ID&date=2026-12-01&durationMinutes=30"
```
Expected: after setting the shift, slots are limited to the shift window; after
adding leave, the slot list for that date is `[]`.

### 4e. Duration-aware slots (Feature C3)
1. Create a booking for a stylist at **09:00** with a **60-minute** service.
2. Fetch slots for that stylist/date with `durationMinutes=30`:
   ```bash
   curl -s "$BASE/slots/available?outletId=$OUTLET_ID&staffId=$STAFF_ID&date=2026-12-01&durationMinutes=30"
   ```
   **Expected:** `09:30` is **absent** (previously it was offered), and `10:00`
   onward is available.
3. Try to create/reschedule a booking at **09:30** for that stylist → **409**
   "overlaps another booking".

### 4f. Payment → booking confirmed (Feature C5)
```bash
BASE=http://localhost:8080/api/v1
# $CUST = customer token, $BOOKING_ID = a booking in SLOT_LOCKED state

# 1. Initiate — returns the payment id + order reference, status PENDING
curl -s -X POST $BASE/payments/initiate -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' -d "{\"bookingId\":\"$BOOKING_ID\"}"

# 2. Verify (dev mode: no RAZORPAY_KEY_SECRET set -> signature check skipped)
curl -s -X POST $BASE/payments/verify -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' -d "{\"paymentId\":\"$PAYMENT_ID\"}"
```
**Expected:** verify returns `status: SUCCESS`, the booking becomes **CONFIRMED**,
and confirmation WhatsApp/email are queued. Verifying twice is safe (idempotent).

### 4g. Reviews & favourites (Feature C6)
```bash
# Review a COMPLETED booking (admin/staff must mark it complete first)
curl -i -X POST $BASE/reviews -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' \
  -d "{\"bookingId\":\"$BOOKING_ID\",\"rating\":5,\"comment\":\"Great service\"}"
# -> 201. Reviewing a non-completed booking or reviewing twice -> 400.

curl -s $BASE/reviews/staff/$STAFF_ID           # public list
curl -s $BASE/favourites -H "Authorization: Bearer $CUST"
curl -i -X POST   $BASE/favourites/$STAFF_ID -H "Authorization: Bearer $CUST"
curl -i -X DELETE $BASE/favourites/$STAFF_ID -H "Authorization: Bearer $CUST"
```

### 4h. Admin & Staff UI (Feature C7)
In the browser at http://localhost:5173:
- **Admin** (`9999999999` / `Admin@123`): check every sidebar item —
  Dashboard, Bookings (Approve/Reject/Complete buttons), Staff (+ Register staff),
  Services (tabs + Add service), **Outlets (set opening/closing time)**,
  Gallery, Analytics, Notifications (broadcast/campaign).
- **Staff** (log in as a staff member you registered): Dashboard shows present
  days, a **status dropdown**, **Check in / Check out**, and today's assigned
  bookings with a *Complete* action; the **Attendance** page shows full history.

### 4i. Frontend white-screen fix (C9)
Use a clean production frontend build so the browser cannot reuse the broken
hashed bundle:

```bash
# From the repository root on chore/dockerise-full-stack
docker compose build --no-cache frontend
docker compose up -d frontend
docker compose ps frontend
```

Then open http://localhost:5173 in a private window (or hard-refresh and clear
site data) and verify:
1. The login page renders; the console has no `Cannot access ... before
   initialization` error and the page is not white.
2. Log in as the seeded admin. Protected API requests include the Bearer token
   and the dashboard renders.
3. Leave the page open until the access token expires (or test with a short
   expiry): several simultaneous API 401s cause one refresh request; queued
   calls retry successfully rather than repeatedly refreshing.
4. Invalidate/delete the refresh token and trigger a 401: auth storage is
   cleared and the browser redirects to `/login`.
5. Navigate through customer/admin/staff routes and refresh a deep link to
   confirm the SPA still initializes normally.

Optional import-graph check from the repository root:

```bash
grep -R "app/store" frontend/src --include='*.ts' --include='*.tsx'
```

**Expected:** `main.tsx` has the only runtime `import { store }`; all component
`RootState` imports say `import type`; `axiosInstance.ts` does not import
`app/store` at all.

### 4j. Coupons and discount lifecycle (Feature C10)

#### Browser happy path
1. Log in as admin → **Coupons** → **Add coupon**. Create `SAVE20`:
   Percentage `20`, minimum spend `500`, maximum discount `150`, a validity
   range containing now, global limit `10`, per-customer limit `1`, Active.
   Leave scopes empty for a global coupon.
2. Confirm the table shows `0 reserved, 0 redeemed`; edit/toggle works and the
   code cannot be duplicated with different case/whitespace.
3. Log in as a customer → **Book Appointment** → select outlet/service/staff/date/
   time → confirmation step → enter `save20` → **Apply**.
4. Confirm the UI displays normalized code, trusted subtotal, discount and final
   total. For a ₹1,000 service, the 20% result is capped: subtotal ₹1,000,
   discount ₹150, total ₹850.
5. Confirm booking. A positive total returns `SLOT_LOCKED`; a 100%/large fixed
   coupon producing zero returns `CONFIRMED` immediately. History retains the
   server total and coupon snapshot.

#### API quote binding
```bash
BASE=http://localhost:8080/api/v1
# $ADMIN / $CUST are access tokens; IDs are real active records.

# Create a global percentage coupon
curl -s -X POST $BASE/coupons -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{
    "code":"SAVE20","name":"Save twenty","discountType":"PERCENTAGE",
    "discountValue":20,"minimumSpend":500,"maximumDiscount":150,
    "validFrom":"2026-08-31T00:00:00Z","validUntil":"2026-12-31T00:00:00Z",
    "usageLimit":10,"perCustomerLimit":1,"isActive":true
  }'

# Advisory quote — does NOT reserve usage
curl -s -X POST $BASE/coupons/validate -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' \
  -d "{\"code\":\" save20 \",\"outletId\":\"$OUTLET_ID\",\"serviceId\":\"$SERVICE_ID\"}"

# Copy data.couponId/subtotalAmount/discountAmount/totalAmount exactly from the
# quote. Booking rechecks them while holding the coupon row lock.
curl -i -X POST $BASE/bookings -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' -d "{
    \"outletId\":\"$OUTLET_ID\",\"staffId\":\"$STAFF_ID\",
    \"serviceId\":\"$SERVICE_ID\",\"scheduledDate\":\"2026-12-01\",
    \"scheduledTime\":\"14:30\",\"couponCode\":\"SAVE20\",
    \"expectedCouponId\":\"$COUPON_ID\",
    \"expectedSubtotalAmount\":1000,\"expectedDiscountAmount\":150,
    \"expectedTotalAmount\":850
  }"
```
Expected: `201`; booking contains all three amounts and `couponCode: SAVE20`.
If an admin changes price/rules after validation, the same booking request fails
with `Coupon quote changed. Please apply the coupon again.` No slot/coupon usage
is leaked.

#### Mandatory edge-case matrix
- **No coupon:** omit code and expected fields → subtotal = total, discount = 0.
- **Minimum/date/active/scope:** below-minimum, expired, inactive, wrong outlet,
  wrong service/package each fail before a slot is locked.
- **Fixed > subtotal / 100%:** discount clamps to subtotal, total 0, booking and
  redemption become CONFIRMED/REDEEMED without payment.
- **Limits/concurrency:** set global limit `1`, validate from two customers, then
  submit both bookings concurrently. Exactly one reserves; the other gets usage
  exhausted. Repeat with per-customer `1` from two tabs for the same customer.
- **Release:** cancel/reject an unpaid discounted booking or wait >11 minutes;
  reservation becomes RELEASED and the limit is available again. Cleanup also
  owner-releases Redis and broadcasts slots.
- **Redeem:** successful payment/admin confirmation changes RESERVED → REDEEMED;
  completion is allowed only from CONFIRMED/IN_PROGRESS. Paid cancellation does
  not restore coupon usage.
- **Reschedule:** subtotal/discount/total/code remain unchanged and the original
  `slot_locked_at` deadline is not extended.
- **Late payment:** settle after booking cancellation/expiry. Payment is SUCCESS
  with `reconciliationRequired: true`; booking stays terminal and coupon is not
  redeemed. Process refund/reconciliation manually.

Optional database audit:
```bash
docker compose exec postgres psql -U salon_user -d salon_db -c \
  "SELECT c.code,r.status,r.discount_amount,r.reserved_at,r.redeemed_at,r.released_at
   FROM coupon_redemptions r JOIN coupons c ON c.id=r.coupon_id
   ORDER BY r.created_at DESC;"
```

## 5. Local testing — Option B: run natively (for active development)

Useful if you want hot-reload instead of rebuilding images.

### Backend
```bash
# Requires JDK 17 + Maven, and a running Postgres + Redis (you can still use
# docker for just those:  docker compose up -d postgres redis minio minio-init )
cd backend
# point Spring at the containers (defaults already target localhost)
export POSTGRES_HOST=localhost REDIS_HOST=localhost \
       REDIS_PASSWORD=changeme_redis_password \
       JWT_SECRET=please_use_a_secret_at_least_32_chars_long
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, Vite proxies /api and /ws to :8080
```
(`vite.config.ts` already proxies `/api` and `/ws` to `http://localhost:8080` in
dev, so no nginx is needed for local dev.)

---

## 6. Stopping / cleaning up
```bash
docker compose down          # stop containers, keep data volumes
docker compose down -v       # also delete Postgres/Redis/MinIO data (fresh start)
```

---

## 7. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `backend` stuck "starting"/unhealthy | First boot is slow; `docker compose logs backend`. If it exits: check Postgres is healthy and `JWT_SECRET` is ≥ 32 chars. |
| Frontend loads but API calls 404/blocked | Ensure you open the **frontend** at `:5173` (nginx proxies `/api`), not the Vite dev server, when running via Docker. |
| `port is already allocated` | Something else uses 5173/8080/5432/6379/9000/9001. Stop it or change the host port mapping in `docker-compose.yml`. |
| Login fails for admin | Use phone `9999999999` / `Admin@123`. Migration `V5` repairs the seeded hash; ensure it applied (`docker compose logs backend | grep -i "V5\|migrat"`). If you seeded a DB before V5, just restart the backend so Flyway runs V5. |
| Image uploads fail | Check `docker compose logs minio-init` shows "minio bucket ready" and the `salon-assets` bucket exists in the MinIO console. |
| Flyway "validate" error on boot | Means an entity/table mismatch; check `docker compose logs backend`. (Verified clean for the current entities.) |
| White screen with `Cannot access ... before initialization` after pulling | The browser/container still has the old hashed bundle. Run `docker compose build --no-cache frontend && docker compose up -d frontend`, then hard-refresh or use a private window. Confirm `axiosInstance.ts` has no store import. |
| Valid coupon suddenly rejected at booking | Catalog/coupon rules changed after Apply, or a final use was consumed. Apply again to obtain a fresh locked quote; this is intentional stale-price protection. |
| Coupon shows usage exhausted after an abandoned booking | Wait for the cleanup cycle (>11 minutes) or verify the booking was cancelled/rejected. Inspect `coupon_redemptions`; a live `RESERVED` record counts toward limits. |
| Payment succeeded but booking stayed cancelled/rejected | This is deliberate late-settlement protection. Check the payment's `reconciliationRequired/reconciliationReason`, then perform a manual refund/reconciliation. |
| Admin Coupons page missing after deploy | Rebuild the frontend image without cache and hard-refresh; confirm V4 migrated and `/api/v1/coupons` returns 200 for an ADMIN token. |

---

## 8. Notes for the reviewer
- A live `docker compose up` was **not run in the authoring environment** (no
  Docker daemon available there). Maven source compilation was blocked by the
  uncached Spring Boot 3.2.0 parent/offline Central access. Frontend dependencies
  are absent, and the sandbox global TypeScript rejects the repository's
  pre-existing `baseUrl` configuration. `git diff --check`, conflict-marker and
  import-graph checks passed; migration/entity contracts, quote binding,
  concurrency, pricing math, and every redemption/payment transition received
  two semantic review passes, with the final review approved and zero findings.
  Run §4i and §4j in a real Docker environment before merging.
- No automated coupon tests were added because they were outside this requested
  implementation scope. Execute the mandatory §4j matrix, especially concurrent
  last-use and stale-quote scenarios.
- The customer payment checkout UI, automatic refunds, and signed webhook
  verification remain follow-ups. Positive-total coupon bookings are correctly
  reserved and priced by the backend, but browser payment completion is not yet
  wired. Full limitations are maintained in `IMPLEMENTATION.md`.
