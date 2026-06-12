# Architecture Overview

## System Design

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTS                              │
│  Customer Portal  │  Staff Portal  │  Admin Dashboard       │
│  (React + TS)     │  (React + TS)  │  (React + TS)          │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTPS / WSS
┌────────────────────────────▼────────────────────────────────┐
│              Spring Boot 3.x — Modular Monolith             │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │   Auth   │ │ Booking  │ │  Staff   │ │  Analytics   │  │
│  │  Module  │ │  Module  │ │  Module  │ │   Module     │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ Gallery  │ │ Payment  │ │  Outlet  │ │ Notification │  │
│  │  Module  │ │  Module  │ │  Module  │ │   Module     │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Shared Infrastructure                  │   │
│  │  Security │ Redis │ WebSocket │ S3 │ Events │ Audit │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
┌──────────▼──────┐        ┌──────────▼──────┐
│  PostgreSQL 15  │        │    Redis 7       │
│  (Primary DB)   │        │  (Slot Locking   │
│                 │        │   + Cache)       │
└─────────────────┘        └─────────────────┘
```

## Slot Locking Flow

```
User clicks "Proceed to Checkout"
        │
        ▼
SlotLockService.tryLock(key, sessionId, TTL=600s)
        │
   ┌────┴────┐
  YES       NO
   │         │
   ▼         ▼
Booking   SlotAlreadyLockedException
status =  → 409 response
SLOT_LOCKED → "Slot just taken" UI
   │
   ▼
Payment flow
   │
 ┌─┴─┐
OK  FAIL
 │    │
 ▼    ▼
CONFIRMED  releaseLock()
 releaseLock()  status=CANCELLED
```

## WhatsApp Notification Flow

```
BookingService.confirmBooking()
        │
        ▼
applicationEventPublisher.publishEvent(BookingConfirmedEvent)
        │
        ▼
BookingEventListener.onBookingConfirmed()  [@Async]
        │
        ▼
NotificationFactory.createConfirmationMessage(booking)
        │
        ▼
WhatsAppNotificationServiceImpl.send()
        │
        ▼
Meta Cloud API POST /messages
        │
        ▼
whatsapp_notifications table (persisted)
```

## Role-Based Access

| Endpoint Pattern | CUSTOMER | STAFF | ADMIN |
|---|---|---|---|
| GET /api/v1/outlets | ✅ | ✅ | ✅ |
| POST /api/v1/bookings | ✅ | ❌ | ✅ |
| PUT /api/v1/bookings/{id}/approve | ❌ | ❌ | ✅ |
| POST /api/v1/staff/attendance/* | ❌ | ✅ | ✅ |
| GET /api/v1/analytics/* | ❌ | ❌ | ✅ |
| POST /api/v1/notifications/broadcast | ❌ | ❌ | ✅ |
