# Dor: Smart Queue System

> Digital queue for a university Student Affairs office.
> Students take a ticket on their phone, staff call them to a desk,
> and a waiting-room screen shows who is being served.

## Problem
Paper queue numbers give students no information about their place in line
and no way to leave the office while they wait.

## Features
- Student: take a ticket, follow position and estimated wait, leave the queue
- Staff: call next student, mark as done or no-show
- Admin: manage services, counters, and users
- Waiting-room screen: who is served now and who is next

*Live updates, early alerts, and statistics arrive in Release 2.*

## Tech stack
| Layer | Technology |
|---|---|
| Frontend | Angular |
| Backend | Java 21, Spring Boot, Spring Security (JWT) |
| Database | PostgreSQL, Flyway migrations |
| Infrastructure | Docker Compose, GitHub Actions |

## Project structure
- `backend/`: Spring Boot server (controller, service, repository layers)
- `frontend/`: Angular interface
- `docs/`: roadmap, requirements, database and API design

## How to run
Coming in Release 1.

## Roadmap
See [docs/roadmap.md](docs/roadmap.md).

## License
MIT
