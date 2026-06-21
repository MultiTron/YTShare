# Backend 100% Class Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining backend class-coverage gap to 100% by adding targeted unit tests for the classes the integration-test plan cannot reach, and deleting two dead exception classes.

**Architecture:** This plan is **Layer 3** of the design (`docs/superpowers/specs/2026-06-21-backend-class-coverage-design.md`). Layer 1 (the `*ControllerIT` integration tests in `docs/superpowers/plans/2026-06-18-backend-service-integration-tests.md`) and Layer 2 (measurement via IntelliJ "Run with Coverage") are **not** part of this plan. Here we add four focused unit tests (`GlobalExceptionHandler`, `WebSocketAuthInterceptor`, `WebSocketConfig`, `FirebaseConfig`), convert the broken `BackendApplicationTests` into a working context-load integration test, and delete `FriendNotFoundException` + `DeviceTokenNotFoundException`.

**Tech Stack:** Spring Boot 4 (Java 21), JUnit 5, Mockito 5 (inline mock maker — `mockStatic` works out of the box, no extra dependency), AssertJ, Spring `ReflectionTestUtils`, Spring Messaging test types.

**Testing model — characterization, not red-first TDD:** Every production class under test already exists and works. Each new test asserts the *current* behavior, so the expected result of the first run is **PASS (green)**. A red first run means a wiring/assertion mismatch to diagnose, not an expected TDD step.

## Global Constraints

- Java version: **21** (from `pom.xml` `<java.version>`).
- New unit tests (Tasks 2–5) are pure JUnit/Mockito — **no Spring context, no Docker** — and run under Surefire via `./mvnw test`. Test class names end in `Test`.
- Task 6 produces an **integration test** (`*IT`, Failsafe, needs Docker) and **depends on `AbstractIntegrationTest`** existing — i.e. the Layer 1 IT plan (`2026-06-18-backend-service-integration-tests.md`, Task 2) must be executed first.
- `GlobalExceptionHandler` body keys/values are exact: `timestamp`, `status` (int `404`/`500`), `error` (`"Not Found"` / `"Internal Server Error"`), `message`.
- `WebSocketConfig` exact values: broker `/topic`, app prefix `/app`, endpoint `/ws`, allowed origin pattern `*`, SockJS enabled.
- `WebSocketAuthInterceptor` exact messages: `"Invalid Firebase token"` (verification failure) and `"Missing Authorization header"` (absent/non-Bearer header); `Bearer ` prefix; authority `ROLE_USER`.
- Deleting the two exceptions must not touch the `ExceptionMessages` enum (constants `FRIEND_NOT_FOUND` / `DEVICE_TOKEN_NOT_FOUND` stay; they are already covered and harmless when unused).
- Do not modify any existing `*ServiceTest`, the IT plan's files, or `pom.xml`.

## File Structure

- Create: `backend/src/test/java/iliev/yt/share/backend/common/handler/GlobalExceptionHandlerTest.java` — unit tests for both handler branches.
- Create: `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptorTest.java` — unit tests for `preSend` branches.
- Create: `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketConfigTest.java` — unit tests for the three configurer methods.
- Create: `backend/src/test/java/iliev/yt/share/backend/security/FirebaseConfigTest.java` — unit tests for `initialize()` (both branches) and `firebaseAuth()`.
- Delete: `backend/src/main/java/iliev/yt/share/backend/user/exception/FriendNotFoundException.java`
- Delete: `backend/src/main/java/iliev/yt/share/backend/devicetoken/exception/DeviceTokenNotFoundException.java`
- Delete: `backend/src/test/java/iliev/yt/share/backend/BackendApplicationTests.java` (broken bare context test)
- Create: `backend/src/test/java/iliev/yt/share/backend/BackendApplicationIT.java` (replacement context-load IT)

---

## Task 1: Delete the two dead exception classes

`FriendNotFoundException` and `DeviceTokenNotFoundException` are never thrown anywhere in the codebase (verified: no `throw new` / `new` references in `backend/src/main` or `backend/src/test`). They cannot be covered by any test. Delete them so they leave the coverage denominator. The `ExceptionMessages` enum constants they referenced stay untouched.

**Files:**
- Delete: `backend/src/main/java/iliev/yt/share/backend/user/exception/FriendNotFoundException.java`
- Delete: `backend/src/main/java/iliev/yt/share/backend/devicetoken/exception/DeviceTokenNotFoundException.java`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing (removal only).

- [ ] **Step 1: Confirm there are no references**

Run: `git grep -n "FriendNotFoundException\|DeviceTokenNotFoundException" -- backend/`
Expected: matches appear **only** inside the two files being deleted. If any other file references them, stop and resolve that first.

- [ ] **Step 2: Delete the two files**

```bash
git rm backend/src/main/java/iliev/yt/share/backend/user/exception/FriendNotFoundException.java
git rm backend/src/main/java/iliev/yt/share/backend/devicetoken/exception/DeviceTokenNotFoundException.java
```

- [ ] **Step 3: Verify the module still compiles**

Run: `./mvnw -q -o clean compile` (drop `-o` if offline cache is cold)
Expected: BUILD SUCCESS — nothing referenced the deleted classes.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: delete unused FriendNotFoundException and DeviceTokenNotFoundException"
```

---

## Task 2: GlobalExceptionHandlerTest

Pure unit test, no Spring context. Covers both `@ExceptionHandler` methods — critically the generic `Exception` → 500 branch that the integration tests never trigger (every IT error is a 404).

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/common/handler/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `GlobalExceptionHandler` (no-arg constructor), its methods `handleGenericNotFoundException(GenericNotFoundException)` and `handleGlobalException(Exception)`, both returning `ResponseEntity<Object>` whose body is a `Map<String, Object>`; `GenericNotFoundException(String)`.
- Produces: nothing later tasks rely on.

- [ ] **Step 1: Write the test**

```java
package iliev.yt.share.backend.common.handler;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericNotFoundException_returns404WithBody() {
        final ResponseEntity<Object> response =
                handler.handleGenericNotFoundException(new GenericNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        @SuppressWarnings("unchecked")
        final Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
                .containsEntry("status", 404)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "missing")
                .containsKey("timestamp");
    }

    @Test
    void handleGlobalException_returns500WithBody() {
        final ResponseEntity<Object> response =
                handler.handleGlobalException(new Exception("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        @SuppressWarnings("unchecked")
        final Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("message", "boom")
                .containsKey("timestamp");
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./mvnw -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS (2 tests).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/common/handler/GlobalExceptionHandlerTest.java
git commit -m "test: cover GlobalExceptionHandler 404 and 500 branches"
```

---

## Task 3: WebSocketAuthInterceptorTest

Pure unit test. Exercises every branch of `preSend`: valid CONNECT token, failed verification, missing header, and a non-CONNECT pass-through. The integration tests never run STOMP frames, so this is the only place `preSend`'s logic is covered.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptorTest.java`

**Interfaces:**
- Consumes: `WebSocketAuthInterceptor(FirebaseAuth)`; `preSend(Message<?>, MessageChannel)`; `FirebaseAuth.verifyIdToken(String)` (throws `FirebaseAuthException`); `FirebaseToken.getUid()`; `FirebaseAuthenticationToken.getUid()`.
- Produces: nothing later tasks rely on.

- [ ] **Step 1: Write the test**

```java
package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import iliev.yt.share.backend.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final MessageChannel channel = mock(MessageChannel.class);
    private FirebaseAuth firebaseAuth;
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        interceptor = new WebSocketAuthInterceptor(firebaseAuth);
    }

    private Message<byte[]> connectMessage(final String authHeader) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void preSend_validToken_setsAuthenticatedUser() throws Exception {
        final FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(firebaseAuth.verifyIdToken("good-token")).thenReturn(token);

        final Message<?> result = interceptor.preSend(connectMessage("Bearer good-token"), channel);

        final StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isInstanceOf(FirebaseAuthenticationToken.class);
        assertThat(((FirebaseAuthenticationToken) accessor.getUser()).getUid()).isEqualTo("uid-1");
    }

    @Test
    void preSend_invalidToken_throws() throws Exception {
        when(firebaseAuth.verifyIdToken("bad-token")).thenThrow(new RuntimeException("verification failed"));

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer bad-token"), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Firebase token");
    }

    @Test
    void preSend_missingHeader_throws() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void preSend_nonConnectCommand_passesThrough() {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }
}
```

**Note on the valid-token assertion:** building the message via `accessor.getMessageHeaders()` yields mutable headers, so `MessageHeaderAccessor.getAccessor(result, …)` returns the same accessor the interceptor mutated with `setUser(...)` — that is why `getUser()` reflects the authentication. The `throws Exception` on the first two tests is required because `verifyIdToken` is stubbed (its signature declares the checked `FirebaseAuthException`); throwing a `RuntimeException` from the stub is allowed and lands in the interceptor's `catch (Exception e)`.

- [ ] **Step 2: Run the test**

Run: `./mvnw -q -Dtest=WebSocketAuthInterceptorTest test`
Expected: PASS (4 tests).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptorTest.java
git commit -m "test: cover WebSocketAuthInterceptor preSend branches"
```

---

## Task 4: WebSocketConfigTest

Pure unit test. Invokes the three `WebSocketMessageBrokerConfigurer` methods with mocked registries and verifies the exact registration calls.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketConfigTest.java`

**Interfaces:**
- Consumes: `WebSocketConfig(WebSocketAuthInterceptor)`; `configureMessageBroker(MessageBrokerRegistry)`; `registerStompEndpoints(StompEndpointRegistry)`; `configureClientInboundChannel(ChannelRegistration)`; `StompEndpointRegistry.addEndpoint(String...) -> StompWebSocketEndpointRegistration`; `StompWebSocketEndpointRegistration.setAllowedOriginPatterns(String...) -> StompWebSocketEndpointRegistration`, `.withSockJS()`; `ChannelRegistration.interceptors(ChannelInterceptor...)`.
- Produces: nothing later tasks rely on.

- [ ] **Step 1: Write the test**

```java
package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    private WebSocketAuthInterceptor interceptor;
    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(mock(FirebaseAuth.class));
        config = new WebSocketConfig(interceptor);
    }

    @Test
    void configureMessageBroker_enablesBrokerAndPrefixes() {
        final MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_registersWsEndpointWithSockJs() {
        final StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        final StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
        verify(registration).setAllowedOriginPatterns("*");
        verify(registration).withSockJS();
    }

    @Test
    void configureClientInboundChannel_registersAuthInterceptor() {
        final ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./mvnw -q -Dtest=WebSocketConfigTest test`
Expected: PASS (3 tests).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/websocket/WebSocketConfigTest.java
git commit -m "test: cover WebSocketConfig broker, endpoint, and channel configuration"
```

---

## Task 5: FirebaseConfigTest

Pure unit test using Mockito static mocking (Mockito 5's default inline mock maker — no dependency change). Covers both branches of `initialize()` and the `firebaseAuth()` bean. The integration tests deliberately disable `FirebaseConfig`, so this is the only coverage it gets.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/security/FirebaseConfigTest.java`

**Interfaces:**
- Consumes: `FirebaseConfig()` (no-arg); private field `firebaseCredentialsJson` (set via `ReflectionTestUtils`); `initialize()` (throws `IOException`); `firebaseAuth() -> FirebaseAuth`. Static methods mocked: `FirebaseApp.getApps()`, `FirebaseApp.initializeApp(FirebaseOptions)`, `GoogleCredentials.fromStream(InputStream)`, `FirebaseAuth.getInstance()`.
- Produces: nothing later tasks rely on.

- [ ] **Step 1: Write the test**

```java
package iliev.yt.share.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class FirebaseConfigTest {

    private FirebaseConfig configWithJson() {
        final FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsJson", "{\"type\":\"service_account\"}");
        return config;
    }

    @Test
    void initialize_noExistingApp_initializesFirebase() throws Exception {
        final FirebaseConfig config = configWithJson();
        final GoogleCredentials credentials = mock(GoogleCredentials.class);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<GoogleCredentials> googleCreds = mockStatic(GoogleCredentials.class)) {

            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCreds.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);

            config.initialize();

            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
        }
    }

    @Test
    void initialize_existingApp_doesNotInitializeAgain() throws Exception {
        final FirebaseConfig config = configWithJson();

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));

            config.initialize();

            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), never());
        }
    }

    @Test
    void firebaseAuth_returnsInstance() {
        final FirebaseConfig config = new FirebaseConfig();
        final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (MockedStatic<FirebaseAuth> staticAuth = mockStatic(FirebaseAuth.class)) {
            staticAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            assertThat(config.firebaseAuth()).isSameAs(firebaseAuth);
        }
    }
}
```

**Note:** `FirebaseOptions.builder()` is **not** mocked — it runs for real and `setCredentials(<mock GoogleCredentials>).build()` succeeds because the mock is non-null. Only the static entry points (`FirebaseApp.*`, `GoogleCredentials.fromStream`, `FirebaseAuth.getInstance`) are stubbed, so no real Firebase network/credential work happens.

- [ ] **Step 2: Run the test**

Run: `./mvnw -q -Dtest=FirebaseConfigTest test`
Expected: PASS (3 tests).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/security/FirebaseConfigTest.java
git commit -m "test: cover FirebaseConfig initialize branches and firebaseAuth bean"
```

---

## Task 6: Convert BackendApplicationTests into a working context-load IT

The current `BackendApplicationTests` is a bare `@SpringBootTest` with no test profile and no datasource, so its context fails to load (this is why `BackendApplication` shows 0% today, and it breaks `./mvnw test`). Replace it with an integration test that extends `AbstractIntegrationTest` (test profile + Testcontainers Postgres + mocked Firebase), so the full context boots and the `BackendApplication` class is instantiated and covered. Renaming to `*IT` keeps it on the Failsafe (Docker-only) side, leaving `./mvnw test` fast and Docker-free.

**DEPENDENCY:** Requires `iliev.yt.share.backend.support.AbstractIntegrationTest` from the Layer 1 IT plan (`2026-06-18-backend-service-integration-tests.md`, Task 2). Do not start this task until that plan is executed.

**Files:**
- Delete: `backend/src/test/java/iliev/yt/share/backend/BackendApplicationTests.java`
- Create: `backend/src/test/java/iliev/yt/share/backend/BackendApplicationIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest` (abstract base: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`, singleton Postgres container, mocked `FirebaseAuth`/`FcmService`/`SimpMessagingTemplate`).
- Produces: nothing later tasks rely on.

- [ ] **Step 1: Delete the broken bare test**

```bash
git rm backend/src/test/java/iliev/yt/share/backend/BackendApplicationTests.java
```

- [ ] **Step 2: Create the replacement IT**

```java
package iliev.yt.share.backend;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class BackendApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Booting the full application context instantiates the @SpringBootApplication
        // class, covering BackendApplication. A failure here is a context-wiring problem.
    }
}
```

- [ ] **Step 3: Run the IT**

Run: `./mvnw -q -Dit.test=BackendApplicationIT verify`
Expected: PASS (1 test). The Postgres container starts, Liquibase applies, the context loads. (Requires Docker running.)

- [ ] **Step 4: Confirm `./mvnw test` no longer runs a context test**

Run: `./mvnw -q test`
Expected: BUILD SUCCESS with no `*IT` classes executed and no container started — the Tasks 2–5 unit tests run and pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: replace broken BackendApplicationTests with context-load IT"
```

---

## Task 7: Merged coverage verification (no code)

Verification only — confirms the design's 100%-class goal is met on the merged report.

**Files:** none.

- [ ] **Step 1: Run the full suite**

Run: `./mvnw -q clean verify`
Expected: BUILD SUCCESS — Surefire runs the unit tests (existing `*ServiceTest` + Tasks 2–5), Failsafe runs every `*IT` (Layer 1 controllers + `InfrastructureIT` + `BackendApplicationIT`). (Requires Docker.)

- [ ] **Step 2: Generate the merged coverage report in IntelliJ**

In IntelliJ: right-click the `backend` test source root → **"Run 'All Tests' with Coverage"** (Docker running). IntelliJ runs `*Test` and `*IT` together and merges them into one coverage view.

- [ ] **Step 3: Confirm 100% class coverage**

In the IntelliJ coverage view (or the regenerated `htmlReport`), confirm every package shows **Class = 100%**. Expected residual non-100% is allowed only for method/line/branch (e.g. `BackendApplication.main()`, config bean-lambdas). If any *class* is still uncovered, identify it and add a focused test following the Task 2–5 pattern.

- [ ] **Step 4: Commit (only if fixups were needed)**

```bash
git add -A
git commit -m "test: finalize backend class coverage"
```

---

## Self-Review

**Spec coverage (against `2026-06-21-backend-class-coverage-design.md`):**
- Layer 1 referenced, not re-specified (executed from its own plan): stated in Architecture + Task 6 dependency. ✔
- Layer 2 measurement via IntelliJ merged run, no pom changes: Task 7, Steps 2–3. ✔
- Layer 3 `FirebaseConfigTest`: Task 5. ✔
- Layer 3 `WebSocketAuthInterceptorTest`: Task 3. ✔
- Layer 3 `WebSocketConfigTest`: Task 4. ✔
- Layer 3 `GlobalExceptionHandlerTest` (500 branch): Task 2. ✔
- `BackendApplicationTests` migrated onto test infrastructure so its context loads: Task 6 (delete bare test + add `BackendApplicationIT extends AbstractIntegrationTest`). ✔
- Delete `FriendNotFoundException` + `DeviceTokenNotFoundException`: Task 1. ✔
- Out of scope honored: no JaCoCo/pom changes, no contrived `main()`/mapper-internal tests, no IT re-spec. ✔

**Placeholder scan:** No TBD/TODO/"similar to"/"add error handling". Every code step shows complete code; every run step gives an exact command and expected result. The two "confirm references / confirm no context test" steps are verification guards, not placeholders.

**Type consistency:** `WebSocketAuthInterceptor(FirebaseAuth)` constructed identically in Tasks 3 and 4. `GlobalExceptionHandler` method names and body keys match the source read during design. `FirebaseConfig` private field name `firebaseCredentialsJson` matches the source. `AbstractIntegrationTest` package/path (`iliev.yt.share.backend.support`) matches the Layer 1 plan. Exact constants (`/topic`, `/app`, `/ws`, `*`, `ROLE_USER`, `"Invalid Firebase token"`, `"Missing Authorization header"`, status `404`/`500`) match the production sources.
