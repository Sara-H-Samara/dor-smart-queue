# Dor: Sprint 1 Requirements

## Sprint goal

A student logs in, takes a ticket, and follows their place in line. Staff call and serve
students, and two desks never receive the same student. Screens refresh automatically
every few seconds.

## Roles

| Role | How the account is created in Sprint 1 |
|---|---|
| Student | Registers from the app |
| Staff | Created in the database when the server starts (seed data) |
| Admin | Created in the database when the server starts (seed data) |

Admin role assignment from the interface comes in Sprint 2.

## User stories

Each story is written as: *As a [role], I want [goal], so that [benefit].*
Acceptance criteria are the checks that prove the story is done.

### US1: Register and log in (Must)
As a student, I want to register and log in, so that I can take tickets.
- A student registers with name, email, and password.
- The email must be unique.
- The password is stored encrypted, never as plain text.
- Login with correct details returns a login token.
- Login with wrong details is rejected with a clear message.

### US2: Role-based access (Must)
As the system, I want each role to reach only its own screens and actions, so that users
can't do what they're not allowed to do.
- Every request except register, login, and the waiting-room data needs a valid token.
- A student calling a staff or admin action gets 403 Forbidden.
- After login, each role is sent to its own screen.

### US3: Manage services (Must)
As an admin, I want to create and edit services, so that students can choose what they need.
- A service has a name, a short code of 1 or 2 capital letters, and is active or inactive.
- The code is unique. For example, R for course registration and D for document requests.
- Students see only active services.

### US4: Manage counters (Must)
As an admin, I want to create counters for each service, so that staff know where they work.
- A counter has a name, belongs to one service, and is active or inactive.

### US5: Take a ticket (Must)
As a student, I want to take a ticket for a service, so that I hold my place in line.
- Ticket numbers start from 1 each day, separately for each service.
- The ticket shown to the student is the service code, a dash, and the number in three
  digits. For example, R-001, R-002, and D-001.
- The same number is never given twice in the same service on the same day.
- A new ticket starts in the WAITING state.
- A student can hold only one active ticket (WAITING or CALLED) at a time.

### US6: See position and estimated wait (Must)
As a student, I want to see my place in line and how long I'll wait, so that I can plan my time.
- Position = number of WAITING tickets ahead of mine in the same service.
- Average service time = the average time of the last 10 finished tickets of the same
  service, measured from when the ticket was called to when it was marked done.
- If fewer than 3 tickets have finished today, the average uses a default of 5 minutes.
- Estimated wait = position x average service time.
- The student screen refreshes automatically every few seconds.

### US7: Leave the queue (Should)
As a student, I want to leave the queue, so that my place goes to someone else.
- A WAITING ticket can move to CANCELLED.
- A CALLED ticket cannot be cancelled.

### US8: Call the next student (Must)
As a staff member, I want to call the next student to my counter, so that I can serve them.
- The staff member chooses their counter first.
- Call next gives the oldest WAITING ticket of that counter's service.
- The ticket moves to CALLED and records the counter and the time.
- If no one is waiting, the staff member sees a clear message.

### US9: Mark served or absent (Must)
As a staff member, I want to record whether the called student was served or did not come
to the desk, so that the line keeps moving.
- Only a CALLED ticket can move to DONE (the student was served) or ABSENT (the student was
  called but did not come to the desk).
- Any other change is rejected with 409 Conflict and a clear message.

### US10: Two desks never get the same student (Must)
As a staff member, I want my call to be safe, so that two desks never serve the same student.
- When two desks call next at the same moment, they receive two different tickets.
- This is checked by an automated test.

### US11: Waiting-room screen (Should)
As a student in the waiting room, I want to see who is being served, so that I know where
the line is.
- The screen needs no login.
- It shows, for each active counter, the ticket being served and the next ticket.
- It refreshes automatically every few seconds.

## Ticket states

```
WAITING --> CALLED --> DONE
   |           |
   |           +--> ABSENT
   +--> CANCELLED
```

A ticket only moves forward. Any change not shown above is rejected.

## Not in Sprint 1

Instant live updates, the distance alert, role assignment from the interface, statistics,
and automatically marking a called student absent after 2 minutes. These are planned for
Sprint 2.
