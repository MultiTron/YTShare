# Backend Class Coverage — Execution Status / Handoff

**As of:** 2026-06-21
**Branch:** `tests/backend-integration-tests`
**Plan:** `2026-06-21-backend-class-coverage.md` · **Design:** `../specs/2026-06-21-backend-class-coverage-design.md`

## Why this note exists

Execution was paused on the work laptop because it has **no Docker** (only Podman,
whose WSL VM is blocked from pulling images by the corporate network). The
remaining work needs a container runtime, so it will continue on a machine with
**Docker**. This note is the resume point.

## DONE — Tasks 1–5 (committed, reviewed clean)

All five are pure unit tests / a deletion — no container needed. Each was
individually reviewed and a final whole-branch review returned **Ready to merge: Yes**.

| Task | Commit | What |
|---|---|---|
| 1 | `5140dc2` | Deleted dead `FriendNotFoundException` + `DeviceTokenNotFoundException` |
| 2 | `bd3f3e5` | `GlobalExceptionHandlerTest` (404 + 500 branches) |
| 3 | `0d9a2b0` | `WebSocketAuthInterceptorTest` (4 preSend branches) |
| 4 | `283e344` | `WebSocketConfigTest` (3 configurer methods) |
| 5 | `f17b8f2` | `FirebaseConfigTest` (initialize both branches + bean) |
| — | `810bcb9` | doc-sync: comment on `setLeaveMutable`, plan code block |

**Plan deviation (already applied & documented):** Task 3's test needed
`accessor.setLeaveMutable(true)` in its `connectMessage` helper — on
spring-messaging 7.x `getMessageHeaders()` freezes headers, which would make the
interceptor's `setUser(...)` throw. Test-only; no production change.

**Run them anytime (no Docker):** from `backend/`, `./mvnw -Dtest=GlobalExceptionHandlerTest,WebSocketAuthInterceptorTest,WebSocketConfigTest,FirebaseConfigTest test`

## TODO — on a Docker machine

1. **Execute the Layer 1 integration-test plan first:**
   `2026-06-18-backend-service-integration-tests.md` (Tasks 1–11). This creates
   `support/AbstractIntegrationTest`, the Failsafe wiring, the `FirebaseConfig`
   `@ConditionalOnProperty` gate, `application-test.properties`, and one
   `*ControllerIT` per domain. Needs Docker (`./mvnw verify`).
2. **Then Task 6 of this plan** — replace the broken bare `BackendApplicationTests`
   with `BackendApplicationIT extends AbstractIntegrationTest`. Depends on step 1.
3. **Then Task 7 of this plan** — IntelliJ "Run with Coverage" over all tests
   (unit + IT) with Docker up; confirm every package shows Class = 100%.

## Uncommitted at pause (decide before pushing)

- **Pre-existing untracked unit tests** under
  `backend/src/test/java/iliev/yt/share/backend/{chat,device,devicetoken,friends,message,notification,user,video}/`
  — the existing `*ServiceTest` + `FcmServiceTest` files. They were untracked at
  session start (not created by this work). **If not committed, they will be
  missing after a pull.** Commit them if you want them on the other machine.
- Generated/local, do NOT commit: `htmlReport/`, `untitled/`, `.idea/*`,
  `.claude/settings*.json`.
- The subagent-driven progress ledger lives in `.git/sdd/progress.md` (local only,
  not part of the repo tree).
