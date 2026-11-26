# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Students (primary):** on their phones, in short and often stressed visits to a university Student Affairs office. They register, log in, take a ticket, see their place and estimated wait, and leave the queue. Mobile web first.
- **Staff (secondary):** at desks, serving students one at a time.
- **Admin (secondary):** configures services and counters.

## Product Purpose

Dor is a digital queue for a university Student Affairs office. Paper queue numbers give students no information about their place in line and no way to leave the office while they wait. Dor replaces them with a ticket on the student's phone. Success: students know their place and wait, and staff serve them without two desks taking the same student.

## Positioning

Honest remote waiting plus conflict-free calling: students see a real position and an estimated wait, so they can leave and come back; staff dispatch guarantees two desks never receive the same student. Both are built in Release 1.

## Operating Context

A university Student Affairs office with several services (for example course registration and document requests) and several counters per service. Tickets show a service code plus a three-digit number, for example R-001, D-001, numbered per service per day. Students use phones; staff use desk screens; a public waiting-room screen shows who is served now.

## Capabilities and Constraints

**Built in Release 1:**
- Login and registration (name, email, password; email unique; password 8 to 72 characters).
- Role-based redirects after login (student, staff, admin).
- Take a ticket: one active ticket (waiting or called) at a time per student, and only when the service has an open counter.
- Student ticket screen: position, estimated wait (the average service time of the latest finished tickets of the same service today, measured from the call to Done; services shorter than 30 seconds or longer than 60 minutes are ignored; 5 minutes per person until there are 3 usable tickets; divided by the number of open counters of the service), called state with the counter name, leave the queue.
- Turn alert: when taking a ticket the student chooses near, medium or far. The student ticket screen shows an alert when the number of people ahead reaches 1 (near), 3 (medium) or 6 (far), times the number of open counters of the service. There is no alert and no alert promise when no counter is open or when the ticket is called.
- Open counters: a counter counts as open when an admin has turned it on and its service is on. The system does not know if a staff member is really at the counter. Admins should turn off counters that have no staff, because an open counter without staff makes the estimated wait too short and the alert too early.
- Staff desk: choose a counter, call next, see the current student and the waiting list, mark done or no-show.
- Admin screen: create, edit, turn on and turn off services and counters.
- Waiting screen at `/waiting-room` (no login) showing who is served at each open counter and the next tickets.
- Live updates on the student ticket screen, the waiting list of the staff desk, the waiting screen and the admin screen. They refresh when the server sends an event. While the live connection is not open, they ask the server every 5 seconds instead.

**Planned (not built; do not present as existing):**
- Early alerts and statistics (Release 2).
- Admin user management from the screen (Release 2).

**Technical:** Angular frontend; Java 21 Spring Boot backend with Spring Security (JWT); PostgreSQL. Planned, not set up yet: Flyway migrations, Docker Compose, GitHub Actions. Staff and admin accounts are seeded in the database. Admin user management from the screen is planned for Release 2.

**Terminology:** service, counter, ticket; ticket states WAITING, CALLED, DONE, NO_SHOW, CANCELLED.

## Evidence on Hand

No logo, brand assets, testimonials, usage data, or customer names exist; do not fabricate any.

## Product Principles

1. The student's phone is the primary surface; design for a quick, anxious glance.
2. Be honest about waiting: show real position and a clearly estimated wait, never false precision.
3. Each role sees only its own screens and actions.
4. Say plainly what happened, especially when no one is waiting or a login fails.
5. Never present planned features as available.

## Accessibility & Inclusion

English-only interface for now. Target WCAG 2.2 AA.
