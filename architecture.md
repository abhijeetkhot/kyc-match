# Architecture Guide

> This document is the source of truth for how this codebase is structured, how data flows through the system, and how Claude Code should generate, modify, and reason about code. Follow these conventions precisely.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Backend & API Design Principles](#backend--api-design-principles)
3. [Folder Structure](#folder-structure)
4. [Data Flow & State Management](#data-flow--state-management)
5. [API Design & Endpoints](#api-design--endpoints)
6. [Testing Strategy](#testing-strategy)
7. [Conventions & Rules](#conventions--rules)

---

## Project Overview

This project is a **full-stack application** consisting of:

- **Frontend** — React / Next.js (App Router) serving the UI and consuming the backend API
- **Backend** — Java REST API (Spring Boot) handling business logic, persistence, and integrations

The two layers are fully decoupled. The frontend communicates with the backend exclusively over HTTP/REST. No business logic lives in the frontend; it is a thin presentation layer.

---

---

## Backend & API Design Principles

These are the non-negotiable principles Claude Code must apply across every theme. They take precedence over convenience or speed of implementation.

---

### 🏗️ Folder Structure & Conventions

1. **One responsibility per layer.** Controllers route, services decide, repositories persist. Code that crosses layer boundaries is always wrong — move it, don't bend the rule.
2. **Package by feature, not by type at scale.** When a domain grows beyond ~5 classes, group by feature (e.g. `user/`, `order/`) rather than keeping a flat `controller/`, `service/` hierarchy. Each feature package owns its controller, service, repository, DTOs, and exceptions.
3. **DTOs are the API contract — entities are not.** Never let a JPA entity leak out of the service layer. DTOs define exactly what the outside world can see and send; entities define the database shape. These are different concerns and must stay separate.
4. **Exceptions are first-class citizens.** Every domain error has a named exception class in `exception/`. Generic `RuntimeException` throws are not acceptable. A single `@ControllerAdvice` translates all exceptions to the standard error envelope.
5. **Config is environment-aware by default.** Every configurable value (timeouts, limits, feature flags, external URLs) lives in `application.yml` and is overridable per profile. Hard-coded values in Java classes are a bug, not a shortcut.

---

### 🔄 Data Flow & State Management

1. **The service layer is the only place decisions are made.** If you find yourself writing an `if` in a controller or a repository, stop and move it to the service. Controllers translate HTTP; repositories translate SQL; services own logic.
2. **Stateless by design.** No `HttpSession`, no in-memory caches shared across requests, no thread-local state that assumes sticky sessions. Every request must be fully self-contained with its JWT.
3. **Commands and queries are different operations (CQRS lite).** Methods that read data must not produce side effects. Methods that write data should return only the minimal result needed (created ID, updated entity). Never mix a write and a broad read in the same service method.
4. **Transactions belong in the service layer, not the controller or repository.** Annotate service methods with `@Transactional`. Repositories are transactional per-call by default — do not add `@Transactional` to repository methods unless you have a specific isolation requirement.
5. **Fail fast on bad input.** Validate all inbound DTOs at the controller boundary using Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.). Never pass an unvalidated object into the service layer.

---

### 🌐 API Design & Endpoints

1. **Resources are nouns, actions are verbs (HTTP methods).** A URL identifies a thing — `/api/v1/orders/42`. The HTTP method says what to do with it. Never put verbs in URLs (`/cancelOrder`, `/processPayment`). Model actions as sub-resources or state transitions (`PATCH /orders/42` with `{ "status": "cancelled" }`).
2. **Consistency beats cleverness.** Every endpoint returns the same envelope shape. Every error returns the same error shape. Every list endpoint supports the same pagination params. Consumers must never have to guess the response structure.
3. **Version at the URL, not the header.** Breaking changes get a new prefix (`/api/v2/`). The old version remains live until consumers have migrated. Never silently change the shape of an existing endpoint.
4. **Be explicit about idempotency.** `GET`, `PUT`, and `DELETE` must be idempotent. `POST` creates a new resource each time. `PATCH` applies a partial update idempotently. Document any endpoint that deviates from this.
5. **Expose only what consumers need.** Response DTOs must include only the fields the client actually uses. Never return internal IDs, audit timestamps, or database internals unless explicitly required. Lean responses reduce coupling and prevent accidental data exposure.

---

### 🧪 Testing Strategy

1. **Test the contract, not the implementation.** Service unit tests assert outcomes (what was returned, what was saved), not which private methods were called. If refactoring internals breaks tests without changing behaviour, the tests are wrong.
2. **Every unhappy path is a first-class test case.** For every service method, write tests for: the happy path, invalid input, not-found, and permission denied. These four cases catch the majority of production bugs.
3. **Integration tests own the HTTP layer.** MockMvc controller tests are the only place to assert HTTP status codes, response headers, and JSON shape. Do not duplicate these assertions in unit tests.
4. **Never mock what you own at the boundary you're testing.** In repository slice tests, use a real (in-memory) database — never mock JPA. In service unit tests, mock repositories — never spin up a database. The tool used matches the layer under test.
5. **Tests are documentation.** Test method names must read as plain-English sentences describing the scenario: `givenInvalidEmail_whenRegisterUser_thenThrowValidationException`. A new developer should understand the system's behaviour by reading test names alone.

---



```
frontend/
├── app/                        # Next.js App Router pages & layouts
│   ├── (auth)/                 # Route group: auth-related pages
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (dashboard)/            # Route group: protected pages
│   │   ├── layout.tsx
│   │   └── page.tsx
│   ├── api/                    # Next.js Route Handlers (BFF layer only)
│   │   └── [...proxy]/route.ts
│   ├── layout.tsx              # Root layout
│   └── page.tsx                # Root page
│
├── components/
│   ├── ui/                     # Primitive, stateless UI components (buttons, inputs)
│   └── features/               # Feature-scoped composite components
│
├── hooks/                      # Custom React hooks (data fetching, state)
├── lib/
│   ├── api/                    # Typed API client functions (fetch wrappers)
│   └── utils/                  # Pure utility functions
├── stores/                     # Global state (Zustand stores)
├── types/                      # Shared TypeScript types & interfaces
└── public/                     # Static assets
```

**Rules Claude Code must follow for the frontend:**
- Pages live in `app/`. Components live in `components/`. Never co-locate business logic inside a page file.
- `components/ui/` holds only dumb, reusable primitives. Feature-specific logic goes in `components/features/`.
- All API calls are made through typed functions in `lib/api/` — never call `fetch` directly from a component.
- Hooks in `hooks/` must start with `use` and encapsulate a single concern.
- Types shared across multiple files go in `types/`. Inline types are only acceptable for one-off, local use.

---

### Backend (`/backend`)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/app/
│   │   │   ├── config/         # Spring configuration classes (Security, CORS, Beans)
│   │   │   ├── controller/     # REST controllers (@RestController)
│   │   │   ├── service/        # Business logic (@Service)
│   │   │   ├── repository/     # Data access layer (@Repository, JPA/JDBC)
│   │   │   ├── model/
│   │   │   │   ├── entity/     # JPA entities mapping to DB tables
│   │   │   │   ├── dto/        # Data Transfer Objects (request/response shapes)
│   │   │   │   └── mapper/     # Entity <-> DTO mappers (MapStruct)
│   │   │   ├── exception/      # Custom exceptions & global exception handler
│   │   │   └── util/           # Stateless utility classes
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/app/
│           ├── controller/     # Integration tests (MockMvc)
│           ├── service/        # Unit tests (JUnit + Mockito)
│           └── repository/     # Repository slice tests (@DataJpaTest)
├── pom.xml
└── Dockerfile
```

**Rules Claude Code must follow for the backend:**
- Controllers are thin. They validate input, call one service method, and return a response. No business logic in controllers.
- Services own all business logic. A service may call other services or repositories, but never another controller.
- Repositories handle only persistence. No business logic, no HTTP concerns.
- Never return JPA entities directly from a controller. Always map to a DTO first.
- Custom exceptions go in `exception/`. Use a global `@ControllerAdvice` to handle them uniformly.
- Environment-specific config goes in the appropriate `application-{profile}.yml`. Secrets must come from environment variables, never hardcoded.

---

## Data Flow & State Management

### Request Lifecycle (Frontend → Backend)

```
User Interaction
      │
      ▼
React Component
      │  calls hook
      ▼
Custom Hook (hooks/)
      │  calls typed API function
      ▼
API Client (lib/api/)
      │  HTTP request
      ▼
Next.js Route Handler (optional BFF proxy)
      │  forwards / transforms
      ▼
Java Spring Controller
      │  delegates
      ▼
Service Layer
      │  reads/writes
      ▼
Repository → Database
```

### Frontend State Layers

| Layer | Tool | Purpose |
|---|---|---|
| Server state | `fetch` + React `cache` / SWR | Remote data, caching, revalidation |
| Global client state | Zustand | Auth session, UI state shared across routes |
| Local component state | `useState` / `useReducer` | Ephemeral, component-scoped state |
| URL state | Next.js `useSearchParams` | Filters, pagination, shareable state |

**Rules:**
- Prefer server state (fetching in Server Components) over client-side fetching wherever possible.
- Zustand stores are for UI state that must survive navigation (e.g. auth, theme, cart). Do not store API response data in Zustand — that belongs in server state.
- Never lift state higher than necessary. Start local, promote only when needed.

### Backend State

The backend is **stateless**. All session context is carried in JWTs passed via the `Authorization` header. No server-side sessions. The database is the single source of truth.

---

## API Design & Endpoints

### Base URL

```
Development:  http://localhost:8080/api/v1
Production:   https://api.yourdomain.com/api/v1
```

### Conventions

- **Versioning**: All endpoints are prefixed with `/api/v1`. Breaking changes require a new version prefix.
- **Resource naming**: Plural nouns. `GET /api/v1/users`, not `/getUsers`.
- **HTTP verbs**: Use semantically correct verbs — `GET` (read), `POST` (create), `PUT` (full replace), `PATCH` (partial update), `DELETE` (remove).
- **Response envelope**: All responses follow this shape:

```json
{
  "success": true,
  "data": { },
  "error": null,
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 200
  }
}
```

- **Error shape**: On failure, `success` is `false`, `data` is `null`, and `error` contains a `code` and `message`:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "No user exists with id 42"
  }
}
```

- **HTTP status codes**:
  - `200` — OK
  - `201` — Created (POST that creates a resource)
  - `204` — No content (DELETE)
  - `400` — Bad request / validation error
  - `401` — Unauthenticated
  - `403` — Forbidden
  - `404` — Not found
  - `409` — Conflict (duplicate resource)
  - `500` — Internal server error

### Pagination

All list endpoints accept `page` (0-indexed) and `size` query params. Default `size` is 20, max is 100.

```
GET /api/v1/users?page=0&size=20&sort=createdAt,desc
```

### Authentication

All protected endpoints require a Bearer token:

```
Authorization: Bearer <jwt>
```

JWTs are issued by `POST /api/v1/auth/login` and contain `userId`, `roles`, and expiry. Refresh via `POST /api/v1/auth/refresh`.

### Example Endpoint Table

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/api/v1/auth/login` | Authenticate and receive JWT | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | No |
| GET | `/api/v1/users` | List users (paginated) | Yes (ADMIN) |
| GET | `/api/v1/users/{id}` | Get user by ID | Yes |
| POST | `/api/v1/users` | Create user | Yes (ADMIN) |
| PATCH | `/api/v1/users/{id}` | Partial update user | Yes |
| DELETE | `/api/v1/users/{id}` | Delete user | Yes (ADMIN) |

---

## Testing Strategy

### Testing Pyramid

```
        /‾‾‾‾‾‾‾‾‾‾‾‾\
       /   E2E Tests   \       ← Few, slow, high confidence
      /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
     / Integration Tests \     ← Moderate, real DB/HTTP
    /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
   /      Unit Tests       \   ← Many, fast, isolated
  /‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\
```

### Frontend Testing

| Type | Tool | What to test |
|---|---|---|
| Unit | Jest + React Testing Library | Hooks, utility functions, isolated components |
| Integration | React Testing Library | Feature components with mocked API responses |
| E2E | Playwright | Critical user journeys (login, checkout, etc.) |

**Rules:**
- Test behavior, not implementation. Assert what the user sees, not which functions were called internally.
- Mock all API calls in component tests using `msw` (Mock Service Worker).
- Every custom hook in `hooks/` must have a unit test.
- E2E tests cover only the top 5–10 critical paths. Do not write E2E tests for edge cases.

### Backend Testing

| Type | Tool | What to test |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service layer logic, mappers, utilities |
| Repository (slice) | `@DataJpaTest` | Queries, custom repo methods against in-memory DB |
| Controller (integration) | MockMvc + `@SpringBootTest` | Full request/response cycle, auth, validation |
| Contract | Spring Cloud Contract | API contract between frontend and backend |

**Rules:**
- Every `@Service` class must have a corresponding unit test class with Mockito mocking all dependencies.
- Repository tests use H2 in-memory database. Never mock the repository in repository tests.
- Controller integration tests must cover: happy path, 400 validation failure, 401 unauthorized, 404 not found.
- Aim for ≥ 80% line coverage on the `service/` package. Coverage is not the goal; meaningful assertions are.
- Test class naming: `{ClassName}Test.java` for unit tests, `{ClassName}IT.java` for integration tests.

---

## Conventions & Rules

### General

- **Claude Code must read this file before generating or modifying any code.** When in doubt, follow what is written here over general best practices.
- Never introduce a new library without a comment explaining why it was chosen.
- No magic numbers. All constants go in a dedicated constants file or enum.
- No commented-out code. Delete it.

### Git

- Branch naming: `feat/`, `fix/`, `chore/`, `docs/` prefixes (e.g. `feat/user-auth`).
- Commit messages follow Conventional Commits: `feat: add JWT refresh endpoint`.
- PRs must be small and focused. One concern per PR.

### Environment Variables

- Frontend: all env vars are prefixed with `NEXT_PUBLIC_` if exposed to the browser.
- Backend: all secrets are injected via environment variables and referenced in `application.yml` using `${VAR_NAME}`. Never hardcode credentials.

### Code Style

- **Java**: Google Java Style Guide. Enforced via Checkstyle.
- **TypeScript/React**: ESLint + Prettier. Enforced via pre-commit hooks.
- All files end with a newline. No trailing whitespace.

---

*Last updated: March 2026. Keep this document in sync as the architecture evolves.*
