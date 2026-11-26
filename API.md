# API reference

Base path: `/api`. Requests and responses use JSON. Dates and times are local server time, for example `2026-01-15T09:30:00`.

## Authentication

Log in with `POST /api/auth/login` and send the token in every protected request:

```
Authorization: Bearer <token>
```

Roles: `STUDENT`, `STAFF`, `ADMIN`. A new account made with register is always a `STUDENT`.

On every request with a token, the server checks the user and the role in the database. A token is only valid while the user exists and still has the role in the token. After a role change, the user must log in again to get a token with the new role.

| Situation | Status |
|---|---|
| No token, or an invalid token | `401` |
| Valid token, but the user no longer exists or the role in the token is not the role in the database | `401`, no body |
| Valid token, wrong role | `403` |

Endpoints that need no token, such as login and the public queue status, ignore an invalid or old token and answer as usual.

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

25 endpoints in total.

### Public (4)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a student account |
| POST | `/api/auth/login` | Get a token |
| GET | `/api/public/queue-status` | Data for the waiting screen |
| GET | `/api/public/queue-stream` | Live updates for the waiting screen |

### Student (5)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/services` | List active services |
| POST | `/api/tickets` | Take a ticket |
| GET | `/api/tickets/me` | Show my active ticket |
| POST | `/api/tickets/me/cancel` | Cancel my waiting ticket |
| GET | `/api/tickets/me/stream` | Live updates for the student screen |

### Staff (7)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/counters` | List open counters (staff and admin) |
| POST | `/api/counters/{counterId}/call-next` | Call the next waiting student |
| GET | `/api/counters/{counterId}/current` | Show the student now at the counter |
| GET | `/api/counters/{counterId}/waiting` | List waiting tickets of the counter's service |
| POST | `/api/tickets/{ticketId}/done` | Mark a called ticket done |
| POST | `/api/tickets/{ticketId}/no-show` | Mark a called ticket as no-show |
| GET | `/api/staff/stream` | Live updates for the staff desk |

### Admin (9)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/stream` | Live updates for the admin screen |
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
{ "serviceId": 1, "distance": "MEDIUM" }
```

`distance` is optional. It tells how far the student is from the counter. Allowed values: `NEAR`, `MEDIUM`, `FAR`. A missing or `null` value means `NEAR`.

Response `201`:

```json
{
  "id": 12,
  "ticketCode": "R-005",
  "serviceId": 1,
  "serviceName": "Registration",
  "status": "WAITING",
  "createdAt": "2026-01-15T09:30:00",
  "distance": "MEDIUM"
}
```

Errors:

| Status | Message |
|---|---|
| `400` | `The request body is not valid JSON.` (also for an unknown `distance` value) |
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
  "hasOpenCounter": true,
  "distance": "MEDIUM",
  "turnIsNear": true,
  "alertAtPeopleAhead": 3
}
```

`estimatedWaitMinutes` is `position` times the average service time, divided by the number of open counters of the service, rounded up to whole minutes. It is `null` when no counter is open.

An open counter is a counter that is on, of a service that is on. The server does not know if a staff member is really at the counter.

The average service time is the average of the latest finished (`DONE`) tickets of the same service today. The service time of a ticket is measured from the call to Done. Services shorter than 30 seconds or longer than 60 minutes are ignored. Until there are 3 usable tickets, the default of 5 minutes per person is used. The server calculates the number. The screen only shows it.

The numbers can be changed with these properties:

| Property | Default | Meaning |
|---|---|---|
| `dor.estimated-wait.default-minutes` | `5` | Minutes per person when there are not enough usable tickets. At least 1. |
| `dor.estimated-wait.recent-count` | `10` | How many of the latest finished tickets are read. At least 1. |
| `dor.estimated-wait.minimum-samples` | `3` | How many usable tickets are needed before the average is used. At least 1 and not more than `recent-count`. |
| `dor.estimated-wait.shortest-service-seconds` | `30` | Services shorter than this are ignored. Not negative. |
| `dor.estimated-wait.longest-service-minutes` | `60` | Services longer than this are ignored. Must be longer than the shortest service. |

The server does not start when a property is not valid.

`turnIsNear` is decided by the server: it is `true` when `peopleAhead` is not more than `alertAtPeopleAhead`.

`alertAtPeopleAhead` is the limit of the `distance` times the number of open counters of the service. The limits are `NEAR` 1, `MEDIUM` 3 and `FAR` 6 people ahead. They can be changed with the `dor.turn-alert.*` properties. A limit of 0 stays 0 for any number of counters.

`alertAtPeopleAhead` is `null` when no counter is open or the ticket is called.

`turnIsNear` is always `false` when no counter is open or the ticket is called.

Examples with an average service time of 4 minutes, a ticket at `position` 6 (5 people ahead) and `distance` `MEDIUM`:

| Open counters | `estimatedWaitMinutes` | `alertAtPeopleAhead` | `turnIsNear` |
|---|---|---|---|
| 1 | 24 | 3 | `false` |
| 2 | 12 | 6 | `true` |
| 0 | `null` | `null` | `false` |

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
  "hasOpenCounter": true,
  "distance": "MEDIUM",
  "turnIsNear": false,
  "alertAtPeopleAhead": null
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

## Live updates

The server can tell a screen when the queue changes, so the screen does not need to ask again and again. It uses server-sent events: one long GET request that stays open and receives small events. The polling endpoints above still work and are not changed.

| Path | Who | Gets |
|---|---|---|
| `GET /api/public/queue-stream` | Anyone, no token | `ticket-changed`, `configuration-changed` |
| `GET /api/tickets/me/stream` | `STUDENT` | `ticket-changed`, `configuration-changed` |
| `GET /api/staff/stream` | `STAFF` | `ticket-changed`, `configuration-changed` |
| `GET /api/admin/stream` | `ADMIN` | `configuration-changed` only |

### How to connect

Send `Accept: text/event-stream`. For the three protected streams, also send `Authorization: Bearer <token>`. The browser `EventSource` cannot send this header, so use `fetch` and read the response body as a stream.

Response `200` with `Content-Type: text/event-stream` and the header `X-Accel-Buffering: no`.

Errors: `401` for no token or an invalid token, `403` for a valid token with the wrong role. For example, an admin token on `/api/staff/stream` returns `403`. `429` when the client address already has too many open streams, `503` when the server has too many open streams in total. Both answers have no body. See [How many streams can be open](#how-many-streams-can-be-open).

The token is checked only when the stream opens. A stream that is already open stays open after the user is deleted or the role changes, until it closes. The next request or the next stream then gets `401`.

### Events

| Event | When it is sent | Sent to |
|---|---|---|
| `ready` | Right after the stream opens | The screen that connected |
| `ticket-changed` | After a ticket was taken, cancelled, called, marked done or marked no-show | Public, student and staff streams |
| `configuration-changed` | After an admin created, renamed, turned on or turned off a service, or created, changed, turned on or turned off a counter | All four streams |

Every event has the data `{}`. An event only says that something changed. It has no names, ticket codes or numbers, and it does not say which ticket or service changed.

```
event:ticket-changed
data:{}

```

Every connected student stream and staff stream gets every `ticket-changed` event, not only the one of the student or counter that changed.

Every 25 seconds by default the server also sends a comment line (`:heartbeat`) to keep the connection open. A comment line is not an event. The time between two heartbeats is the property `dor.streams.heartbeat-interval-millis` (default `25000`).

### What the screen does

After `ready` and after every event, the screen asks for the data it needs with a normal request:

| Screen | Request |
|---|---|
| Waiting screen | `GET /api/public/queue-status` |
| Student ticket | `GET /api/tickets/me` while the student has a ticket, and `GET /api/services` after any event while the student has no ticket |
| Staff desk | `GET /api/counters/{counterId}/waiting` |
| Admin | `GET /api/admin/services` and `GET /api/admin/counters` |

The screen must ask again after every `ready`. Events that came while the screen was not connected are not sent again.

### When events are sent

An event is sent only after the change is saved in the database. A request that fails sends nothing. This includes `400`, `404` and `409` answers, and `call-next` when nobody is waiting (`204`).

### How long a stream stays open

The server closes each stream after 10 minutes. The screen must open a new stream, with a valid token for the protected streams, and ask for the data again after `ready`. The server also closes all streams when it stops.

### How many streams can be open

All four stream paths share the same two limits. The limits are checked after the token, and before the stream is created.

| Limit | Default | Property | Answer when reached |
|---|---|---|---|
| Open streams for one client address | `100` | `dor.streams.max-streams-per-address` | `429` |
| Open streams in total | `1000` | `dor.streams.max-total-streams` | `503` |

The client address is the address of the direct connection. The headers `X-Forwarded-For` and `Forwarded` are not used. Behind a proxy or a load balancer, all clients share the address of the proxy. When both limits are reached, the answer is `429`.

The answer has no body and no `Retry-After` header. A slot is freed when the stream ends for any reason: the client closes it, it reaches its lifetime, a send fails, or the server stops. Streams that are already open are not affected by a refusal.

## Known limitations

See [Known limitations in the README](README.md#known-limitations).
