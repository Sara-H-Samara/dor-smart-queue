# Dor: API Endpoints (Sprint 1)

All endpoints start with `/api`. Requests and responses use JSON.

**Access** says who can call the endpoint:
- **Public:** no login needed.
- **Signed in:** any user with a valid login token.
- **Student**, **Staff**, **Admin:** only users with that role.

The login token is sent in every request header as `Authorization: Bearer <token>`.

## 1. Accounts (US1, US2)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `fullName`, `email`, `password` | 201: `id`, `fullName`, `email`, `role` |
| POST | `/api/auth/login` | Public | `email`, `password` | 200: `token`, `fullName`, `role` |
| GET | `/api/auth/me` | Signed in | none | 200: `id`, `fullName`, `email`, `role` |

## 2. Services (US3)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| GET | `/api/services` | Signed in | none | 200: list of active services: `id`, `name`, `code` |
| GET | `/api/admin/services` | Admin | none | 200: list of all services, including `active` |
| POST | `/api/admin/services` | Admin | `name`, `code` | 201: the new service |
| PUT | `/api/admin/services/{id}` | Admin | `name`, `code`, `active` | 200: the updated service |

## 3. Counters (US4)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| GET | `/api/counters` | Staff, Admin | none | 200: list of active counters: `id`, `name`, `serviceId`, `serviceName` |
| GET | `/api/admin/counters` | Admin | none | 200: list of all counters, including `active` |
| POST | `/api/admin/counters` | Admin | `name`, `serviceId` | 201: the new counter |
| PUT | `/api/admin/counters/{id}` | Admin | `name`, `serviceId`, `active` | 200: the updated counter |

## 4. Student tickets (US5, US6, US7)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| POST | `/api/tickets` | Student | `serviceId` | 201: ticket view |
| GET | `/api/tickets/me/active` | Student | none | 200: ticket view, or 204 if the student has no active ticket |
| POST | `/api/tickets/{id}/cancel` | Student (own ticket) | none | 200: ticket view |

**Ticket view:**

| Field | Example | Meaning |
|---|---|---|
| `id` | 42 | Ticket id |
| `displayNumber` | `R-004` | Service code and number |
| `serviceName` | Course registration | The service |
| `status` | WAITING | Current state |
| `position` | 3 | Waiting tickets ahead (only while WAITING) |
| `estimatedWaitMinutes` | 12 | Position x average service time (only while WAITING) |
| `counterName` | Desk 1 | The desk that called the student (only when CALLED) |

## 5. Staff desk (US8, US9, US10)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| POST | `/api/counters/{counterId}/call-next` | Staff | none | 200: called ticket, or 204 if no one is waiting |
| GET | `/api/counters/{counterId}/current` | Staff | none | 200: ticket being served, or 204 if none |
| POST | `/api/tickets/{id}/done` | Staff | none | 200: ticket view |
| POST | `/api/tickets/{id}/absent` | Staff | none | 200: ticket view |

**Called ticket:** `id`, `displayNumber`, `studentName`, `status`, `calledAt`.

**Two desks calling at once (US10):** the server picks the oldest waiting ticket. If another
desk took it at the same moment, the server tries the next waiting ticket, so each desk gets
a different student.

## 6. Waiting room (US11)

| Method | Path | Access | Request body | Success response |
|---|---|---|---|---|
| GET | `/api/waiting-room` | Public | none | 200: one row per active counter |

**Row:** `counterName`, `serviceName`, `servingNumber` (for example `R-003`, or empty),
`nextNumber` (for example `R-004`, or empty).

## Errors

Every error returns the same shape:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Only a called ticket can be marked done.",
  "timestamp": "2026-11-08T09:15:00"
}
```

| Code | When |
|---|---|
| 400 Bad Request | A required field is missing or invalid |
| 401 Unauthorized | No login token, or the token is invalid |
| 403 Forbidden | The user's role is not allowed to call this endpoint |
| 404 Not Found | The ticket, service, or counter does not exist |
| 409 Conflict | A rule is broken: an invalid status change, a second active ticket, or a duplicate email or service code |
