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
git clone https://gitlab.com/kgaur624/salon-management-platform
cd salon-management-platform
cp .env.example .env
# Fill in your credentials in .env
docker-compose up -d
```

- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:5173
- MinIO Console: http://localhost:9001

## Project Structure

```
salon-management-platform/
├── backend/          # Spring Boot 3.x application
├── frontend/         # React 18 + TypeScript application
├── database/         # Flyway SQL migrations
├── docker/           # Dockerfiles
├── docker-compose.yml
├── .env.example
└── docs/
    └── architecture.md
```
