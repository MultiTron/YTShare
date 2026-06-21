# Backend 100% Class Coverage — Design

**Date:** 2026-06-21
**Status:** Approved (pending spec review)
**Scope:** `backend/` (Spring Boot 4, Java 21)

## Goal

Reach **100% class coverage** of the backend, measured as a **merged report** over
both the unit tests (`*Test`, Surefire) and the integration tests (`*IT`,
Failsafe). "Class coverage" = every *live* class has at least one method executed
by a test. Where a class is the actual subject of a test, the test must exercise
its real logic/branches — not merely instantiate it ("meaningful coverage").

Method/line/branch coverage are **not** targeted at 100%; they stay partial by
design.

## Baseline

From the coverage report (`htmlReport/index.html`, generated 2026-06-21):
class coverage **70.2% (66/94)** — 28 classes have zero coverage. The existing
`*ServiceTest` Mockito unit tests cover the services but **mock the mappers**, so
the real MapStruct `*MapperImpl`s are not exercised, and controllers, the security
package, websocket, the exception handler, config classes, and
`BackendApplication` are entirely untested.

## Strategy — three layers

### Layer 1 — Integration tests (existing plan, executed as-is)

The repo already contains an approved, fully-written plan:
`docs/superpowers/plans/2026-06-18-backend-service-integration-tests.md`
(Tasks 1–11). It adds one `*ControllerIT` per domain driving real HTTP through
filter → controller → service → real MapStruct mapper → repository →
Testcontainers PostgreSQL, plus the shared `AbstractIntegrationTest` base, the
Failsafe wiring, and the single `@ConditionalOnProperty` gate on `FirebaseConfig`.

Executing that plan covers, in one stroke:
- all 8 controllers,
- all `*MapperImpl`s (real mappers, not mocked),
- the real security chain: `FirebaseTokenFilter`, `SecurityConfig`,
  `SecurityUtils`, `FirebaseAuthenticationToken`,
- `GlobalExceptionHandler` (the 404 / `GenericNotFoundException` branch),
- `PersistenceConfig`, entities,
- most `*NotFoundException` classes (thrown on not-found HTTP paths).

This is Layer 1 and produces the largest coverage jump. It is **not re-specified
here** — it is executed from its own plan.

### Layer 2 — Measurement (IntelliJ merged report)

No `pom.xml` / JaCoCo changes. Coverage is read from a single IntelliJ
**"Run with Coverage"** over the whole test source set: IntelliJ runs both
`*Test` and `*IT` classes as plain JUnit and merges them into one coverage view
automatically.

**Requirement:** Docker must be running during the coverage run, because the ITs
start the Testcontainers Postgres.

Trade-off accepted: this is an IDE action, not reproducible from the command line
or CI. That is acceptable for this goal (a one-time/periodic report, no gate).

### Layer 3 — Targeted unit tests for IT-unreachable classes

After Layer 1, these classes are still not (meaningfully) covered. Each gets a
focused unit test.

| Class | Why the ITs miss it | Test |
|---|---|---|
| `security/FirebaseConfig` | Deliberately disabled in the `test` profile (`firebase.enabled=false`) | `FirebaseConfigTest` — Mockito static-mock `FirebaseApp` / `GoogleCredentials` / `FirebaseAuth`; assert `initialize()` builds options + initializes when no app exists, and `firebaseAuth()` returns the instance |
| `websocket/WebSocketAuthInterceptor` | `preSend` only fires on real STOMP CONNECT frames (transport out of IT scope) | `WebSocketAuthInterceptorTest` — three cases: valid token → `setUser` with `FirebaseAuthenticationToken`; bad token (`verifyIdToken` throws) → `IllegalArgumentException`; missing/`null` header → `IllegalArgumentException` |
| `websocket/WebSocketConfig` | Configurer methods not exercised in the MOCK web environment | `WebSocketConfigTest` — invoke `configureMessageBroker`, `registerStompEndpoints`, `configureClientInboundChannel` with mocked registries; verify the expected registry calls |
| `common/handler/GlobalExceptionHandler` (500 branch) | Every IT error is a 404; the generic `Exception` → 500 branch is never hit | `GlobalExceptionHandlerTest` — call `handleGlobalException(new Exception("boom"))`, assert 500 + body shape |
| `BackendApplication` | `BackendApplicationTests` is a bare `@SpringBootTest` with no test profile / datasource, so its context fails to load today | Migrate `BackendApplicationTests` to extend `AbstractIntegrationTest` so the context actually loads (covers the class) |

### Dead-code cleanup

`user/exception/FriendNotFoundException` and
`devicetoken/exception/DeviceTokenNotFoundException` are **never thrown anywhere**
in the codebase (verified: no `throw new ...` / `new ...` references in main or
test). They are dead code; no test can cover them naturally.

**Action: delete both classes** (approved). This also removes them from the
denominator, so they stop dragging the class-coverage number.

## Execution order

1. Execute the existing IT plan (`2026-06-18-backend-service-integration-tests.md`,
   Tasks 1–11). Biggest jump.
2. Delete the two dead exception classes.
3. Add the Layer 3 unit tests and migrate `BackendApplicationTests`.
4. Run IntelliJ "Run with Coverage" over all tests (Docker up) and read the
   merged report. Address any residual red class the report reveals.

## Deliverables

- All Layer 1 files (from the existing IT plan — not re-listed here).
- New unit tests:
  - `backend/src/test/java/iliev/yt/share/backend/security/FirebaseConfigTest.java`
  - `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptorTest.java`
  - `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketConfigTest.java`
  - `backend/src/test/java/iliev/yt/share/backend/common/handler/GlobalExceptionHandlerTest.java`
- Modified: `backend/src/test/java/iliev/yt/share/backend/BackendApplicationTests.java`
  (extend `AbstractIntegrationTest`).
- Deleted: `FriendNotFoundException.java`, `DeviceTokenNotFoundException.java`.

## Out of scope

- A JaCoCo build gate / CI coverage enforcement (report-only, via IntelliJ).
- 100% method/line/branch coverage.
- Re-specifying the integration-test plan (it has its own approved spec + plan).
- Contrived tests for config bean-lambdas, `BackendApplication.main()`, or
  generated `*MapperImpl` internals.
- Frontend or Android tests.

## Dependencies / assumptions

- The existing IT plan is executed first; Layer 3's `BackendApplicationTests`
  migration depends on `AbstractIntegrationTest` existing (created in that plan,
  Task 2).
- Docker is available on the machine running the coverage pass.
