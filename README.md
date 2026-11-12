# Dor: Smart Queue System

> Digital queue for a university Student Affairs office.
> Students take a ticket on their phone, staff call them to a desk,
> and a waiting-room screen shows who is being served.

## Problem
Paper queue numbers give students no information about their place in line
and no way to leave the office while they wait.

## Features
**Available now**
- Register and log in, with student, staff, and admin roles
- Admin: manage services and counters
- Student: take a ticket and see position and estimated wait
- Student: ticket screen
- Student: leave the queue

**Planned**
- Staff: call next student, mark as done or no-show
- Admin: manage users
- Waiting-room screen: who is served now and who is next

*Live updates, early alerts, and statistics arrive in Release 2.*

## Tech stack
| Layer | Technology |
|---|---|
| Frontend | Angular |
| Backend | Java 21, Spring Boot, Spring Security (JWT) |
| Database | PostgreSQL |

Planned: Flyway migrations, Docker Compose, GitHub Actions.

## Project structure
- `backend/`: Spring Boot server (controller, service, repository, entity, dto, security, config)
- `frontend/`: Angular interface

## How to run
In progress - setup instructions will be added soon.

## License
MIT
