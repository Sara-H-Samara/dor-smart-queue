# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Students (primary):** on their phones, in short and often stressed visits to a university Student Affairs office. They register, log in, and (planned) take a ticket and wait. Mobile web first.
- **Staff (secondary, planned screen):** at desks, serving students one at a time.
- **Admin (secondary):** configures services and counters.

## Product Purpose

Dor is a digital queue for a university Student Affairs office. Paper queue numbers give students no information about their place in line and no way to leave the office while they wait. Dor replaces them with a ticket on the student's phone. Success: students know their place and wait, and staff serve them without two desks taking the same student.

## Positioning

Honest remote waiting plus conflict-free calling: students see a real position and an estimated wait, so they can leave and come back; staff dispatch guarantees two desks never receive the same student. Both are planned, not yet built.

## Operating Context

A university Student Affairs office with several services (for example course registration and document requests) and several counters per service. Tickets show a service code plus a three-digit number, for example R-001, D-001, numbered per service per day. Students use phones; staff use desk screens; a waiting-room screen is planned.

## Capabilities and Constraints

**Built today:**
- Login and registration (name, email, password; email unique; password 8 to 72 characters).
- Role-based redirects after login (student, staff, admin).
- Admin screen for managing services and counters.

**Planned (not built; do not present as existing):**
- Student ticket flow: take a ticket, see position and estimated wait, leave the queue.
- Staff desk: choose a counter, call next, mark served or no-show.
- Waiting-room screen showing who is served now and who is next.
- Automatic refresh every few seconds; live updates, early alerts and statistics arrive in Release 2.

**Technical:** Angular frontend; Java 21 Spring Boot backend with Spring Security (JWT); PostgreSQL. Planned, not set up yet: Flyway migrations, Docker Compose, GitHub Actions. Staff and admin accounts are seeded in the database in Sprint 1; admin role assignment from the UI comes in Sprint 2.

**Terminology:** service, counter, ticket; ticket states WAITING, CALLED, CANCELLED, plus served and no-show outcomes.

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
