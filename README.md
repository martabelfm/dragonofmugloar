# Mugloar Mission Control

A full-stack client for the [Dragons of Mugloar](https://dragonsofmugloar.com/) engineering assignment. The application supports manual play, explainable strategy recommendations, and optional server-controlled automation through one consistent game interface.

## Contents

1. [Architecture](#architecture)
2. [Key design decisions](#key-design-decisions)
3. [Game mechanics](#game-mechanics)
4. [Technology](#technology)
5. [Run locally](#run-locally)
6. [Local development](#local-development)
7. [Requirements coverage](#requirements-coverage)
8. [API](#api)
9. [Verification](#verification)
10. [Project conventions](#project-conventions)

## Architecture

```text
Browser
  Angular UI
  NgRx Signal Store
       |
       | same-origin /api requests
       v
Frontend container
  nginx static hosting and reverse proxy
       |
       v
Spring Boot API
  web             HTTP validation and problem responses
  application     game sessions, use cases, history, automation
  strategy        Conservative and High-risk decision policies
  domain          immutable game records and strategy values
  infrastructure  external API client and response normalization
       |
       v
Dragons of Mugloar API
```

The browser communicates only with this application's `/api` endpoints. In Docker, nginx forwards those requests to the backend container; during frontend development, the Angular development proxy performs the same role. The browser never needs direct access to the external game API.

The backend uses a layered, ports-and-adapters-oriented structure:

- `web` accepts and validates HTTP input.
- `application` coordinates sessions and game actions through `GamePort`.
- `strategy` converts the current game state into one explainable next action.
- `domain` contains framework-independent records and enums.
- `infrastructure` implements the external Mugloar API boundary.

Recommendations and automation share the same `DecisionEngine`. The UI renders the current backend recommendation, while `/auto/step` executes that exact recommendation. This prevents manual guidance and automated play from developing separate rules.

## Key design decisions

- **Backend-owned strategy state.** Strategy mode is stored with the game session as `OFF`, `CONSERVATIVE`, or `HIGH_RISK`, so refreshing a game route preserves the selected mode.
- **Single upstream boundary.** `MugloarClient` contains transport concerns, Jackson decoding, encrypted advertisement handling, and upstream error translation.
- **Deterministic tests.** Automated tests use fake game ports and local HTTP stubs rather than the live game service.
- **No unsafe retries.** Turn-changing requests are not retried automatically because the upstream API provides no idempotency key; a timed-out request may already have consumed a turn.
- **In-memory sessions.** No database is needed for the assignment. `/games/{gameId}` restores a session while the backend is running; restarting the backend clears local sessions.
- **Translation-ready UI copy.** Static frontend text is kept in `frontend/src/app/i18n/en.json`. Dynamic mission, shop, and recommendation text remains owned by the game API.

## Game mechanics

Observed mechanics, action costs, the turn-versus-level difficulty model, and open questions are documented in [GAME_MECHANICS.md](GAME_MECHANICS.md). Strategy rules are documented separately in [STRATEGY.md](STRATEGY.md).

## Technology

| Area | Technology |
|---|---|
| Frontend | Angular 22, TypeScript 6, RxJS 7 |
| State | NgRx Signal Store |
| Backend | Java 25, Spring Boot 4.1 |
| API documentation | springdoc-openapi / Swagger UI |
| JSON | Jackson 3 with a custom advertisement deserializer |
| Backend tests | JUnit 5, AssertJ, Spring Boot Test, WireMock |
| Frontend tests | Vitest through Angular's unit-test builder |
| Packaging | Docker Compose, multi-stage Docker builds, nginx |

## Run locally

### Prerequisite

- Docker Desktop

No host installation of Java, Maven, Node.js, or npm is required. The multi-stage Docker builds provide Maven and JDK 25 for the backend build, Node.js for the frontend build, Java for the backend runtime, and nginx for frontend hosting.

From the repository root:

```bash
docker compose up --build
```

When both services are healthy, open:

| Service | URL |
|---|---|
| Application | <http://localhost:4200> |
| Backend API | <http://localhost:8081> |
| Swagger UI | <http://localhost:8081/swagger-ui.html> |
| Health check | <http://localhost:8081/actuator/health> |

Stop the application with `Ctrl+C`. Containers can then be removed with:

```bash
docker compose down
```

## Local development

This section is optional and is only relevant when running the services directly on the host instead of through Docker Compose.

Host-based development requires:

- JDK 25; Maven is supplied by the included Maven Wrapper
- Node.js 24 or 26 with npm

Start the backend on port 8080:

```bash
cd backend
./mvnw spring-boot:run
```

In another terminal, start the Angular development server:

```bash
cd frontend
npm ci
npm start
```

The Angular development proxy forwards `/api` requests to `http://localhost:8080`.

## Requirements coverage

| Assignment requirement | Implementation | Verification |
|---|---|---|
| Start a game; view and solve advertisements | Angular game board backed by the Spring Boot game API | `GameServiceTest` covers the application flow through a deterministic fake port. |
| Buy shop items; display score, gold, lives, and level | Responsive status header and shop controls | Service tests cover affordability and purchase tracking. |
| Frontend state management | NgRx Signal Store owns game, loading, error, and automation state | Frontend unit tests and production build. |
| Reach at least 1,000 points | Conservative strategy produces one explainable decision per turn | `automatedStepsReachTheRequiredScoreAndRecordEveryAction` verifies a complete 1,000-point run. |
| Error handling and validation | Bean Validation, RFC 9457 problem responses, and visible UI errors | Controller and service tests cover invalid input and invalid actions. |
| Responsive interface | Desktop and mobile layouts, keyboard-accessible controls, and native dialogs | Manual smoke checklist below. |
| Unit tests | Focused backend strategy/service tests and Angular component/store tests | Commands in [Verification](#verification). |

## API

All application endpoints use the `/api/games` base path.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/games` | Start and hydrate a game |
| `GET` | `/api/games/{gameId}` | Read a locally known game session |
| `POST` | `/api/games/{gameId}/refresh` | Refresh advertisements and shop data |
| `POST` | `/api/games/{gameId}/ads/{adId}/solve` | Solve an advertisement |
| `POST` | `/api/games/{gameId}/shop/{itemId}/purchase` | Purchase a shop item |
| `POST` | `/api/games/{gameId}/reputation` | Investigate reputation |
| `PUT` | `/api/games/{gameId}/strategy-mode` | Select `OFF`, `CONSERVATIVE`, or `HIGH_RISK` |
| `POST` | `/api/games/{gameId}/auto/step` | Execute the current backend recommendation |

Strategy behavior and its UI integration are documented in [STRATEGY.md](STRATEGY.md).

## Verification

The following commands run tests directly on the host and therefore use the optional local development toolchain described above. They are not required to start the application through Docker Desktop.

Backend:

```bash
cd backend
./mvnw verify
```

Frontend:

```bash
cd frontend
npm ci
npm test
npm run build
```

The automated tests do not call the live Mugloar service.

### Manual smoke test

- Start a game and refresh `/games/{gameId}`; verify that the session and selected strategy remain available.
- Solve a mission and buy an affordable item; verify score, gold, lives, and level update together.
- Attempt an unavailable mission and an unaffordable purchase; verify a readable error without losing the current screen.
- Select Conservative and High risk; verify recommendations update and automation can start and stop.
- Check desktop and narrow mobile layouts for overlapping controls, clipped content, and keyboard accessibility.

## Project conventions

- Keep business decisions in `DecisionEngine` and the strategy classes, with focused tests for every policy change.
- Keep upstream response quirks and decoding inside `MugloarClient` and `AdvertisementDeserializer`.
- Validate untrusted HTTP input in the web layer and current-game rules in the application layer.
- Treat generated directories (`backend/target`, `frontend/dist`, and `frontend/node_modules`) as build output.
- Preserve existing action history so completed manual and automated runs remain inspectable.
