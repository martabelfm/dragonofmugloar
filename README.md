# Mugloar Mission Control

A full-stack client for the [Dragons of Mugloar](https://dragonsofmugloar.com/) engineering assignment. It supports deliberate manual play and optional server-led guidance, while keeping all turn-changing calls behind a single backend API.

## Requirements coverage

| Assignment requirement | Implementation | Verification |
|---|---|---|
| Start a game; view and solve ads | Angular game board backed by `POST /api/games` and solve endpoints | `GameServiceTest` covers the end-to-end application flow with a fake upstream port. |
| Buy shop items; show score, gold, lives | Compact shop, header status panel, and purchase endpoint | Purchase affordability and purchase counts are unit tested. |
| State management | NgRx Signal Store owns the game, request state, errors, and automation state | Frontend tests and production build. |
| Reach 1,000+ points | `DecisionEngine` produces one explainable action; `auto/step` runs that same decision used by guidance | `automatedStepsReachTheRequiredScoreAndRecordEveryAction` proves a 1,000-point run against a deterministic game port. |
| Error handling and validation | Bean validation for route IDs and request bodies; RFC 9457 problem responses; UI error state | Service tests cover invalid purchases; controller validation is enforced at the HTTP boundary. |
| Responsive and cross-browser UI | CSS Grid/Flex layouts, semantic controls, native dialog, and a mobile breakpoint | Test in current Chrome, Firefox, and Safari/Edge before submission; see [manual QA](#manual-qa). |
| Unit tests | JUnit/AssertJ backend tests and Vitest Angular tests | Run the commands below. |

## Architecture

```text
Angular UI + NgRx Signal Store
             │
             ▼
      Spring Boot REST API
       ├─ GameService: validation, session and turn history
       ├─ DecisionEngine: one server-side recommendation policy
       └─ MugloarClient: normalized external API boundary
             │
             ▼
      Dragons of Mugloar API
```

The browser does not call the game API directly. This prevents CORS exposure, normalizes inconsistent upstream response shapes, and makes the decision policy independently testable. Manual guidance and automation share exactly the same server-side decision, avoiding duplicated rules in TypeScript.

Sessions are intentionally in memory: the live game API remains the game authority and no database is needed for the assignment. A game URL (`/games/{gameId}`) restores its local session while the backend remains running; a backend restart clears sessions.

## Run locally

Prerequisites: Docker Desktop, or JDK 25 and Node.js 24/26.

```bash
docker compose up --build
```

Open `http://localhost:4200`. The API is available at `http://localhost:8081`; OpenAPI UI is at `http://localhost:8081/swagger-ui.html`.

For development, run `./mvnw spring-boot:run` in `backend`, then `npm ci && npm start` in `frontend`. The Angular proxy expects the backend on port 8080.

## Test and build

```bash
cd backend
./mvnw verify

cd ../frontend
npm test
npm run build
```

Normal tests never call the live game API, so they remain deterministic. The live API is intentionally not retried for turn-changing requests because it has no idempotency key: a timed-out response may already have consumed a turn.

## API surface

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/games` | Start and hydrate a game |
| GET | `/api/games/{gameId}` | Read the locally known game view |
| POST | `/api/games/{gameId}/refresh` | Refresh advertisements |
| POST | `/api/games/{gameId}/ads/{adId}/solve` | Solve an advertisement |
| POST | `/api/games/{gameId}/shop/{itemId}/purchase` | Purchase an item |
| POST | `/api/games/{gameId}/reputation` | Investigate reputation |
| PUT | `/api/games/{gameId}/strategy-mode` | Select Conservative or High-risk guidance |
| POST | `/api/games/{gameId}/auto/step` | Execute one recommended action |

## Manual QA

Before handing in, use this short smoke checklist in Chrome, Firefox, and one WebKit-based browser (Safari or Edge):

- Start a game, refresh `/games/{gameId}`, and confirm the game remains available.
- Solve an ad, buy an affordable item, and check score/gold/lives update together.
- Try an unavailable ad and an unaffordable item; confirm the visible error is clear and the UI stays usable.
- At desktop and narrow mobile widths, confirm no controls overlap and keyboard focus reaches every action.
- Turn guidance on/off and run/stop an automated step sequence.

## Project conventions

- `domain` holds immutable game records; `application` holds use cases and policy; `web` is the HTTP adapter.
- Controllers validate untrusted input; `GameService` validates current-game rules before an upstream call.
- `DecisionEngine` is the sole place to alter recommendation policy. Keep its behavior covered by focused tests when tuning strategy.
- Keep upstream API quirks isolated in `MugloarClient` rather than spreading parsing logic across the application.
