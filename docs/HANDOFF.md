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
| Login fails for admin | Use phone `9999999999` / `Admin@123`. If you ran `down -v`, the seed re-applies on next `up`. |
| Image uploads fail | Check `docker compose logs minio-init` shows "minio bucket ready" and the `salon-assets` bucket exists in the MinIO console. |
| Flyway "validate" error on boot | Means an entity/table mismatch; check `docker compose logs backend`. (Verified clean for the current entities.) |

---

## 8. Notes for the reviewer
- A live `docker compose up` was **not run in the authoring environment** (no
  Docker daemon available there). Every change was cross-checked by tracing the
  runtime paths and the entity↔schema alignment. Please do a local bring-up to
  confirm before merging.
- The admin/staff pages are **placeholders** so the build and routes work; full
  functionality is a good next PR.
