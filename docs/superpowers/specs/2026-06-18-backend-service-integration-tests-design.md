# Backend Service Integration Tests — Design

**Date:** 2026-06-18
**Status:** Approved (pending spec review)
**Scope:** `backend/` (Spring Boot 4, Java 21)

## Goal

Add full-stack HTTP integration tests covering every backend service that has a
REST controller. Each test drives a real HTTP request through the complete
runtime stack — security filter → controller → service → mapper → repository →
real PostgreSQL — so that wiring, security, serialization, MapStruct mappers,
Liquibase schema, and JPA persistence are all exercised together. These tests
**complement** the existing `*ServiceTest` Mockito unit tests; they do not
replace them.

## Boundary

```
MockMvc request
  └─> FirebaseTokenFilter (REAL)        ← FirebaseAuth bean is MOCKED
        └─> Spring Security chain (REAL)
              └─> Controller (REAL)
                    └─> Service (REAL)
                          ├─> Mapper (REAL, MapStruct)
                          ├─> Repository (REAL, Spring Data JPA)
                          │     └─> PostgreSQL (REAL, Testcontainers + Liquibase)
                          ├─> FcmService (MOCKED)
                          └─> SimpMessagingTemplate (MOCKED)
```

**Mocked (external boundaries only):**
- `FirebaseAuth` — token verification (no network / real Firebase project).
- `FcmService` — push notifications (Firebase Messaging static API).
- `SimpMessagingTemplate` — WebSocket broadcast.

Everything else runs for real.

## Coverage

One `*ControllerIT` class per domain that exposes a controller:

| Test class | Controller under test |
|---|---|
| `UserControllerIT` | `UserController` |
| `VideoControllerIT` | `VideoController` |
| `ChatControllerIT` | `ChatController` |
| `MessageControllerIT` | `MessageController` |
| `FriendshipControllerIT` | `FriendshipController` |
| `DeviceControllerIT` | `DeviceController` |
| `DeviceTokenControllerIT` | `DeviceTokenController` |
| `UserPreferencesControllerIT` | `UserPreferencesController` |

**Excluded:** `FcmService` has no controller and is a thin wrapper over the
Firebase Messaging static API. It is mocked in these tests and remains covered
by the existing `FcmServiceTest`. It does not get an HTTP integration test.

## Test Infrastructure

### Shared base class — `AbstractIntegrationTest`

Located at `backend/src/test/java/iliev/yt/share/backend/support/AbstractIntegrationTest.java`.

Responsibilities:
- `@SpringBootTest(webEnvironment = WebEnvironment.MOCK)`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- Provides a **singleton** `PostgreSQLContainer` shared across all test classes
  (started once in a static initializer and intentionally not stopped, so the
  JVM reuses a single container for the whole test run — fastest option).
  Connection wired via `@ServiceConnection` (Spring Boot Testcontainers support)
  or `@DynamicPropertySource` if `@ServiceConnection` proves fiddly.
- Liquibase runs the real changelogs against the container at startup;
  `spring.jpa.hibernate.ddl-auto=validate` therefore passes against the real
  schema (this is the whole point of using Postgres rather than H2).
- Declares the shared mocks via `@MockitoBean`: `FirebaseAuth`, `FcmService`,
  `SimpMessagingTemplate`.
- `@Transactional` so each test method rolls back its DB changes. MockMvc
  executes in the test thread, so the test-managed transaction wraps the
  request and rollback is clean.
- Injects `MockMvc` and an `ObjectMapper` for subclasses.

### Test resources — `src/test/resources/application-test.properties`

- `firebase.enabled=false` (see Production Change below).
- Datasource left to Testcontainers wiring.
- Reduced log noise (e.g. Liquibase / Hibernate SQL off).

### Auth & data helpers — `support/`

- `TestAuthSupport` (or helper methods on the base class):
  - `authHeaderFor(String uid, String email)` — stubs
    `firebaseAuth.verifyIdToken(token)` to return a Mockito `FirebaseToken` stub
    exposing the given `uid` / `email`, and returns the matching
    `Authorization: Bearer <token>` header value.
  - Tests attach this header so the **real** `FirebaseTokenFilter` performs
    authentication end-to-end.
- Fixture builders: small helpers that persist seed entities (users, chats,
  videos, etc.) directly via the real repositories inside `@BeforeEach`, so each
  test starts from a known DB state.

## Production Change (single, minimal)

`FirebaseConfig.initialize()` (a `@PostConstruct`) reads
`firebase.credentials.json` and calls `GoogleCredentials.fromStream(...)`, which
throws when credentials are absent/invalid — this breaks Spring context startup
under test.

**Change:** annotate the `FirebaseConfig` class with
`@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)`.

- Production behavior is unchanged: the property is absent, so `matchIfMissing`
  keeps the config active and Firebase initializes exactly as today.
- The `test` profile sets `firebase.enabled=false`, so `FirebaseConfig` (and its
  failing `@PostConstruct` and real `FirebaseAuth` bean) is skipped; the
  `@MockitoBean FirebaseAuth` is used instead.

This is the only change to `src/main`.

## Build / Run Wiring

Integration tests run as integration tests under **Failsafe**, separate from the
Surefire unit tests:

- Naming convention `*IT` so Surefire ignores them and Failsafe picks them up.
- Add `maven-failsafe-plugin` to `backend/pom.xml` with `integration-test` and
  `verify` goals bound to the lifecycle.
- Add Testcontainers dependencies (test scope):
  `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter`
  (BOM managed by the Spring Boot parent where possible).

**Run commands:**
- `./mvnw test` — unit tests only (`*Test` / `*Tests`), unchanged.
- `./mvnw verify` — runs unit tests, then the `*IT` integration tests.
- `./mvnw failsafe:integration-test` — integration tests only.

Requires Docker available on the machine running `verify` (the project already
uses docker-compose for dev Postgres).

## What Each Test Class Asserts

For every endpoint of the controller under test:
- **Happy path:** correct HTTP status, response JSON body shape/values, and
  resulting DB state (row created/updated/deleted, relationships persisted).
- **Not found:** requesting a missing resource yields the domain exception mapped
  to the correct status/body by `GlobalExceptionHandler`.
- **Validation:** invalid request bodies (where DTOs declare constraints) return
  the expected `400` with the handler's error shape.
- **Auth required:** requests without a valid `Authorization` header return
  `401` (non-public endpoints).

Domain-specific assertions:
- **MessageControllerIT:** creating a message verifies `SimpMessagingTemplate`
  broadcast to `/topic/chat/{chatId}` and `FcmService.sendPushNotification(...)`
  invocations for recipient device tokens (via the mocks); also covers
  chat-not-found and sender-not-found paths.
- **ChatControllerIT:** participant resolution (all-present vs partially-missing
  → `ChatParticipantsNotFoundException`), add-participants, and the
  `chat_participants` join-table state.
- **UserControllerIT:** includes `getCurrentUser` driven by the authenticated
  Firebase uid resolved through `SecurityUtils`.

## File Layout

```
backend/src/test/java/iliev/yt/share/backend/
  support/
    AbstractIntegrationTest.java
    TestAuthSupport.java
  user/UserControllerIT.java
  video/VideoControllerIT.java
  chat/ChatControllerIT.java
  message/MessageControllerIT.java
  friends/FriendshipControllerIT.java
  device/DeviceControllerIT.java
  devicetoken/DeviceTokenControllerIT.java
  user/preferences/UserPreferencesControllerIT.java
backend/src/test/resources/
  application-test.properties
```

(The existing `*ServiceTest` unit-test files remain in place, untouched.)

## Out of Scope

- Rewriting or removing the existing Mockito `*ServiceTest` unit tests.
- WebSocket transport / STOMP end-to-end testing (broadcast is asserted via the
  mocked `SimpMessagingTemplate`).
- Real Firebase token verification or real FCM delivery.
- Frontend or Android tests.
