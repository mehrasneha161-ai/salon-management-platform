# 💈 Salon Management Platform

A full-stack, production-grade Salon & Beauty Parlour Business Management Web Application.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Ant Design + Redux Toolkit |
| Backend | Spring Boot 3.x (Java 17) — Modular Monolith |
| Database | PostgreSQL 15 + Redis 7 |
| Storage | AWS S3 / MinIO |
| Notifications | Meta WhatsApp Cloud API |
| Containerization | Docker + Docker Compose |

## Modules

- **Customer Portal** — Auth, Booking, Slot Picker, Gallery, History, Reviews
- **Staff Portal** — Attendance, Status Toggle, Assigned Bookings
- **Admin Dashboard** — Booking Approval, Staff Management, Analytics, Content, Broadcasts
- **Core Engine** — Slot Locking (Redis), WhatsApp Notifications, Payment Gateway

## Quick Start

```bash
git clone https://github.com/mehrasneha161-ai/salon-management-platform
cd salon-management-platform
cp .env.example .env
# Fill in your credentials in .env (JWT_SECRET must be >= 32 chars)
docker compose up -d --build
```

`docker compose up` brings up the full stack: **Postgres, Redis, MinIO
(+ auto-created `salon-assets` bucket), the Spring Boot backend, and the
nginx-served React frontend**. The backend runs Flyway migrations on startup
(schema + a seeded admin user), and the frontend's nginx reverse-proxies
`/api` and `/ws` to the backend, so everything works from a single origin.

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001

**Default admin login** (seeded by migration): phone `9999999999`, password `Admin@123`.

## Project Structure

```
salon-management-platform/
├── backend/                     # Spring Boot 3.x application
│   ├── Dockerfile               # multi-stage build -> slim non-root JRE image
│   └── src/main/resources/db/migration/   # Flyway SQL migrations (on classpath)
├── frontend/                    # React 18 + TypeScript application
│   ├── Dockerfile               # Vite build -> nginx
│   └── nginx.conf               # SPA fallback + /api & /ws reverse proxy
├── docker-compose.yml
├── .env.example
└── docs/
    └── architecture.md
```
