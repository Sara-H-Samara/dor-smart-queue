# API reference

Base path: `/api`. Requests and responses use JSON. Dates and times are local server time, for example `2026-01-15T09:30:00`.

## Authentication

Log in with `POST /api/auth/login` and send the token in every protected request:

```
Authorization: Bearer <token>
```

Roles: `STUDENT`, `STAFF`, `ADMIN`. A new account made with register is always a `STUDENT`.

| Situation | Status |
|---|---|
| No token, or an invalid token | `401` |
| Valid token, wrong role | `403` |

## Errors

Errors from the application use this shape:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "You already have an active ticket.",
  "timestamp": "2026-01-15T09:30:00"
}
```

A validation failure returns `400` and names the invalid fields:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value for: email, password.",
  "timestamp": "2026-01-15T09:30:00"
}
```

A body that is not valid JSON returns `400` with the message `The request body is not valid JSON.`

## Ticket statuses

`WAITING`, `CALLED`, `DONE`, `NO_SHOW`, `CANCELLED`.

| From | Can change to |
|---|---|
| `WAITING` | `CALLED`, `CANCELLED` |
| `CALLED` | `DONE`, `NO_SHOW` |
| `DONE`, `NO_SHOW`, `CANCELLED` | nothing |

A ticket code is the service code, a dash and the number with three digits, for example `A-007`. Numbers start again each day, per service.

## Endpoints

21 endpoints in total.

### Public (3)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a student account |
| POST | `/api/auth/login` | Get a token |
| GET | `/api/public/queue-status` | Data for the waiting screen |

### Student (4)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/services` | List active services |
| POST | `/api/tickets` | Take a ticket |
| GET | `/api/tickets/me` | Show my active ticket |
| POST | `/api/tickets/me/cancel` | Cancel my waiting ticket |

### Staff (6)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/counters` | List open counters (staff and admin) |
| POST | `/api/counters/{counterId}/call-next` | Call the next waiting student |
| GET | `/api/counters/{counterId}/current` | Show the student now at the counter |
| GET | `/api/counters/{counterId}/waiting` | List waiting tickets of the counter's service |
| POST | `/api/tickets/{ticketId}/done` | Mark a called ticket done |
| POST | `/api/tickets/{ticketId}/no-show` | Mark a called ticket as no-show |

### Admin (8)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/services` | List all services |
| POST | `/api/admin/services` | Create a service |
| PUT | `/api/admin/services/{id}` | Rename a service |
| PATCH | `/api/admin/services/{id}/active` | Turn a service on or off |
| GET | `/api/admin/counters` | List all counters |
| POST | `/api/admin/counters` | Create a counter |
| PUT | `/api/admin/counters/{id}` | Change name and service of a counter |
| PATCH | `/api/admin/counters/{id}/active` | Turn a counter on or off |

## Public endpoints

### POST /api/auth/register

Request:

```json
{
  "fullName": "Test Student",
  "email": "student@example.com",
  "password": "example-pass-1"
}
```

`fullName`: 1 to 100 characters. `email`: valid email, up to 150 characters, saved in lower case. `password`: 8 to 72 characters.

Response `201`:

```json
{
  "id": 4,
  "fullName": "Test Student",
  "email": "student@example.com",
  "role": "STUDENT"
}
```

Errors: `400` invalid fields, `409` `Email is already registered.`

### POST /api/auth/login

Request:

```json
{
  "email": "student@example.com",
  "password": "example-pass-1"
}
```

Response `200`:

```json
{
  "token": "<jwt-token>",
  "fullName": "Test Student",
  "role": "STUDENT"
}
```

Errors: `400` invalid fields, `401` `Email or password is incorrect.`

### GET /api/public/queue-status

No token needed. Shows the open counters with their newest called ticket today, up to 3 next waiting tickets, and how many more are waiting.

Response `200`:

```json
{
  "counters": [
    { "counterName": "Counter 1", "serviceName": "Registration", "ticketCode": "R-004" },
    { "counterName": "Counter 2", "serviceName": "Registration", "ticketCode": null }
  ],
  "nextTickets": [
    { "ticketCode": "R-005", "serviceName": "Registration" },
    { "ticketCode": "R-006", "serviceName": "Registration" }
  ],
  "moreWaitingCount": 0
}
```

`ticketCode` of a counter is `null` when no ticket was called there today.

## Student endpoints

### GET /api/services

Response `200`: active services only.

```json
[
  { "id": 1, "name": "Registration", "code": "R", "active": true, "hasOpenCounter": true }
]
```

### POST /api/tickets

Request:

```json
{ "serviceId": 1 }
```

Response `201`:

```json
{
  "id": 12,
  "ticketCode": "R-005",
  "serviceId": 1,
  "serviceName": "Registration",
  "status": "WAITING",
  "createdAt": "2026-01-15T09:30:00"
}
```

Errors:

| Status | Message |
|---|---|
| `400` | `This service is not available.` |
| `404` | `Service not found.` |
| `409` | `You already have an active ticket.` |
| `409` | `No counter is open for <service name> right now.` |
| `409` | `Please try again.` |

### GET /api/tickets/me

Shows the student's ticket of today that is `WAITING` or `CALLED`.

Response `200` for a waiting ticket:

```json
{
  "ticketCode": "R-005",
  "serviceName": "Registration",
  "status": "WAITING",
  "position": 2,
  "peopleAhead": 1,
  "estimatedWaitMinutes": 10,
  "counterName": null,
  "hasOpenCounter": true
}
```

`estimatedWaitMinutes` is `position` times 5, and `null` when no counter is open.

Response `200` for a called ticket:

```json
{
  "ticketCode": "R-005",
  "serviceName": "Registration",
  "status": "CALLED",
  "position": null,
  "peopleAhead": null,
  "estimatedWaitMinutes": null,
  "counterName": "Counter 1",
  "hasOpenCounter": true
}
```

Errors: `404` `You do not have an active ticket.`

### POST /api/tickets/me/cancel

No body. Response `204` with no content.

Errors: `404` `You do not have an active ticket.`, `409` `A called ticket cannot be cancelled.`

## Staff endpoints

### GET /api/counters

Open counters: the counter is on and its service is on. Sorted by counter name. Also allowed for admin.

Response `200`:

```json
[
  { "id": 1, "name": "Counter 1", "serviceId": 1, "serviceName": "Registration" }
]
```

### POST /api/counters/{counterId}/call-next

No body. Calls the oldest waiting ticket of the counter's service for today.

Response `200`:

```json
{
  "id": 12,
  "ticketCode": "R-005",
  "studentName": "Test Student",
  "status": "CALLED",
  "calledAt": "2026-01-15T09:40:00",
  "counterName": "Counter 1"
}
```

Response `204` with no content when nobody is waiting.

Errors:

| Status | Message |
|---|---|
| `404` | `Counter not found.` |
| `409` | `This counter is not active.` |
| `409` | `The service of this counter is not active.` |
| `409` | `This counter is already serving a student.` |

### GET /api/counters/{counterId}/current

Response `200`: the same body as call-next, for the student now called at the counter.
Response `204` with no content when the counter has no called student today.

Errors: `404` `Counter not found.`

### GET /api/counters/{counterId}/waiting

Lists the waiting tickets of the counter's service for today, oldest first, up to 20. `waitingCount` is the full number of waiting tickets.

Response `200`:

```json
{
  "waitingCount": 2,
  "tickets": [
    { "ticketCode": "R-006", "createdAt": "2026-01-15T09:35:00" },
    { "ticketCode": "R-007", "createdAt": "2026-01-15T09:36:00" }
  ]
}
```

Errors: `404` `Counter not found.`

### POST /api/tickets/{ticketId}/done

No body. Response `200`: the same body as call-next, with `"status": "DONE"`.

Errors: `404` `Ticket not found.`, `409` `Only a called ticket can be marked done.`

### POST /api/tickets/{ticketId}/no-show

No body. Response `200`: the same body as call-next, with `"status": "NO_SHOW"`.

Errors: `404` `Ticket not found.`, `409` `Only a called ticket can be marked as no-show.`

## Admin endpoints

### GET /api/admin/services

Response `200`: all services sorted by name.

```json
[
  { "id": 1, "name": "Registration", "code": "R", "active": true }
]
```

### POST /api/admin/services

Request:

```json
{ "name": "Registration", "code": "r" }
```

`name`: 1 to 100 characters, trimmed. `code`: 1 or 2 English letters, saved in upper case.

Response `201`: a service, as in the list above.

Errors: `400` invalid fields, `409` `Service name already exists.`, `409` `This code is already used by another service.`, `409` `Service name or code already exists.`

### PUT /api/admin/services/{id}

Request:

```json
{ "name": "New name" }
```

Response `200`: the service.

Errors: `400` invalid fields, `404` `Service not found.`, `409` `Service name already exists.`

### PATCH /api/admin/services/{id}/active

Request:

```json
{ "active": false }
```

Response `200`: the service.

Errors: `400` invalid fields, `404` `Service not found.`, `409` `Finish the called student first.` (when turning off while a student of this service is called today)

### GET /api/admin/counters

Response `200`:

```json
[
  { "id": 1, "name": "Counter 1", "active": true, "serviceId": 1, "serviceName": "Registration" }
]
```

### POST /api/admin/counters

Request:

```json
{ "name": "Counter 1", "serviceId": 1 }
```

`name`: 1 to 100 characters, trimmed.

Response `201`: a counter, as in the list above.

Errors: `400` invalid fields or `This service is not available.`, `404` `Service not found.`, `409` `Counter name already exists.`

### PUT /api/admin/counters/{id}

Request: the same as create.

Response `200`: the counter.

Errors: `400` invalid fields or `This service is not available.`, `404` `Counter not found.` or `Service not found.`, `409` `Counter name already exists.`, `409` `Finish the called student first.` (when changing the service while a student is called at the counter)

### PATCH /api/admin/counters/{id}/active

Request:

```json
{ "active": false }
```

Response `200`: the counter.

Errors: `400` invalid fields, `404` `Counter not found.`, `409` `Finish the called student first.` (when turning off while a student is called at the counter)

## Known limitations

See [Known limitations in the README](README.md#known-limitations).
