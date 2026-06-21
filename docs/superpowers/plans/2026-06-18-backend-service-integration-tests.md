# Backend Service Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full-stack HTTP integration tests (MockMvc → security filter → controller → service → mapper → repository → real PostgreSQL) for every backend service that exposes a REST controller.

**Architecture:** A shared `AbstractIntegrationTest` base boots the full Spring context with `@SpringBootTest`, a singleton Testcontainers PostgreSQL instance (real Liquibase schema, `ddl-auto=validate`), and Mockito replacements for the three external boundaries (`FirebaseAuth`, `FcmService`, `SimpMessagingTemplate`). Each domain gets one `*IT` class. Tests run under Failsafe (`mvn verify`), separate from the existing Surefire unit tests.

**Tech Stack:** Spring Boot 4 (Java 21), JUnit 5, Spring MockMvc, Spring Security Test, Mockito (Spring `@MockitoBean`), Testcontainers (postgresql + junit-jupiter), AssertJ, Liquibase, MapStruct.

**Testing model — characterization, not red-first TDD:** The production endpoints already exist and work. Each test asserts the *current* behavior of existing code, so the expected result of the first run is **PASS (green)**, not a red failure. A failing first run means a wiring/behavior problem to diagnose and fix, not an expected TDD step. The only genuinely new code is test infrastructure and one tiny production annotation.

## Global Constraints

- Java version: **21** (copied from `pom.xml` `<java.version>`).
- Endpoints have **no `/api` prefix** and **no servlet context-path**. Real paths: `/users`, `/videos`, `/chats`, `/messages`, `/friendships`, `/devices`, `/device-tokens`, `/user-preferences`.
- All `*NotFoundException` classes extend `GenericNotFoundException`, mapped by `GlobalExceptionHandler` to **HTTP 404** with body `{ timestamp, status:404, error:"Not Found", message }`. `ChatParticipantsNotFoundException` also extends it → 404.
- DTOs are plain Java records with **no Bean Validation annotations** — do not write "invalid body → 400" assertions; none exist.
- All non-public endpoints require authentication. Unauthenticated requests are rejected by Spring Security before reaching the controller (assert `is4xxClientError()`; see Task 3 note).
- Mocked beans (the ONLY mocks): `FirebaseAuth`, `FcmService`, `SimpMessagingTemplate`. Everything else is real.
- Test class naming: `*IT` (Failsafe), so they do NOT run under `./mvnw test` and DO run under `./mvnw verify`.
- Existing `*ServiceTest` unit tests must remain untouched.
- Entity non-null fields that fixtures must always set:
  - `User`: `firebaseUid`, `email`, `firstName`, `lastName` (all `length=128`, unique on `firebaseUid` + `email`).
  - `Video`: `title` (≤128), `url`, `thumbnailUrl`.
  - `Message`: `content`, `status` (enum `DeliveryStatus`), `chat`, `sender`.
  - `Friendship`: `status` (enum `FriendshipStatus`); `user`/`friend` are nullable FKs.
  - `DeviceToken`: `fcmToken` (unique), `platform` (≤32), `user`.
  - `UserPreferences`: booleans default false; `user` via `user_id`.
  - `Device`: all columns nullable except FK semantics; created through a `UserPreferences`.

---

## Task 1: Build wiring + Firebase test gate

Adds the Testcontainers dependencies, the Failsafe plugin, the one production annotation that lets the context start without Firebase credentials, and the test profile properties. No tests run yet; the deliverable is "the project still compiles and `verify` is green with zero ITs."

**Files:**
- Modify: `backend/pom.xml` (dependencies + build plugins)
- Modify: `backend/src/main/java/iliev/yt/share/backend/security/FirebaseConfig.java`
- Create: `backend/src/test/resources/application-test.properties`

**Interfaces:**
- Produces: a `test` Spring profile in which `firebase.enabled=false` disables `FirebaseConfig`; Failsafe runs `**/*IT.java`; Testcontainers postgresql + junit-jupiter on the test classpath.

- [ ] **Step 1: Add Testcontainers test dependencies to `backend/pom.xml`**

Insert these two dependencies inside `<dependencies>` (versions are managed by the Spring Boot parent BOM — do not add `<version>`):

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add the Failsafe plugin to `backend/pom.xml`**

Inside `<build><plugins>` (alongside the existing plugins), add:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

(Failsafe's default include pattern is `**/*IT.java`, `**/IT*.java`, `**/*ITCase.java`. Surefire's default is `**/*Test.java`/`**/*Tests.java`, so `*IT` classes are NOT run by `test`.)

- [ ] **Step 3: Gate `FirebaseConfig` behind a property**

In `backend/src/main/java/iliev/yt/share/backend/security/FirebaseConfig.java`, add the import and the class-level annotation. Production behavior is unchanged because `matchIfMissing = true`.

Add import:

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
```

Add the annotation directly above `public class FirebaseConfig {` (keep the existing `@Configuration`):

```java
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {
```

- [ ] **Step 4: Create `backend/src/test/resources/application-test.properties`**

```properties
# Disable real Firebase initialization in tests; a mock FirebaseAuth bean is supplied instead.
firebase.enabled=false

# Datasource is provided dynamically by the Testcontainers Postgres in AbstractIntegrationTest.
# Liquibase runs the real changelogs; Hibernate validates against them.
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.enabled=true

# Quiet the test output.
spring.jpa.show-sql=false
logging.level.liquibase=WARN
logging.level.org.hibernate.SQL=WARN
```

- [ ] **Step 5: Verify it compiles and `verify` is green with no ITs**

Run: `./mvnw -q clean verify`
Expected: BUILD SUCCESS. Unit tests (`*Test`) run under Surefire and pass; Failsafe finds zero `*IT` classes and passes. (Requires Docker running, though no container starts yet.)

- [ ] **Step 6: Commit**

```bash
git add backend/pom.xml backend/src/main/java/iliev/yt/share/backend/security/FirebaseConfig.java backend/src/test/resources/application-test.properties
git commit -m "test: add testcontainers + failsafe wiring and firebase test gate"
```

---

## Task 2: Integration-test base class + auth helper + smoke test

Creates the shared base (singleton Postgres container, MockMvc, the three mocked beans, per-test transaction rollback) and an auth helper, proving the whole stack boots against a real DB and that security is wired.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/support/AbstractIntegrationTest.java`
- Create: `backend/src/test/java/iliev/yt/share/backend/support/InfrastructureIT.java`

**Interfaces:**
- Produces:
  - `AbstractIntegrationTest` — abstract base. Protected members available to subclasses:
    - `protected MockMvc mockMvc;`
    - `protected ObjectMapper objectMapper;`
    - `@MockitoBean protected FirebaseAuth firebaseAuth;`
    - `@MockitoBean protected FcmService fcmService;`
    - `@MockitoBean protected SimpMessagingTemplate messagingTemplate;`
    - `protected String authHeaderFor(String uid, String email) throws FirebaseAuthException` — stubs `firebaseAuth.verifyIdToken("token-" + uid)` to return a `FirebaseToken` mock with the given uid/email and returns `"Bearer token-" + uid`.
  - All subclasses are `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional` (inherited).

- [ ] **Step 1: Create `AbstractIntegrationTest`**

```java
package iliev.yt.share.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import iliev.yt.share.backend.notification.FcmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    // Singleton container: started once in the static initializer and intentionally
    // never stopped, so it is shared across every IT class in the run. Ryuk removes
    // it when the JVM exits.
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected FirebaseAuth firebaseAuth;

    @MockitoBean
    protected FcmService fcmService;

    @MockitoBean
    protected SimpMessagingTemplate messagingTemplate;

    /**
     * Stubs Firebase token verification for {@code uid}/{@code email} and returns the
     * matching Authorization header value. The REAL FirebaseTokenFilter authenticates the request.
     */
    protected String authHeaderFor(final String uid, final String email) throws FirebaseAuthException {
        final String token = "token-" + uid;
        final FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(email);
        when(firebaseAuth.verifyIdToken(token)).thenReturn(firebaseToken);
        return "Bearer " + token;
    }
}
```

- [ ] **Step 2: Create the smoke test `InfrastructureIT`**

```java
package iliev.yt.share.backend.support;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InfrastructureIT extends AbstractIntegrationTest {

    @Test
    void contextLoads_andContainerIsRunning() {
        org.assertj.core.api.Assertions.assertThat(POSTGRES.isRunning()).isTrue();
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        // No Authorization header -> Spring Security rejects before the controller runs.
        mockMvc.perform(get("/users/all"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void authenticatedRequest_reachesController() throws Exception {
        mockMvc.perform(get("/users/all")
                        .header("Authorization", authHeaderFor("uid-smoke", "smoke@example.com")))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 3: Run the smoke test**

Run: `./mvnw -q -Dit.test=InfrastructureIT failsafe:integration-test`
Expected: PASS. The Postgres container starts, Liquibase applies the changelogs, Hibernate validates the schema, and both the unauthenticated (4xx) and authenticated (200, empty list) requests behave as asserted.

(If `failsafe:integration-test` alone does not compile test sources in your Maven version, use `./mvnw -q -Dit.test=InfrastructureIT verify`.)

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/support/
git commit -m "test: add integration-test base class, auth helper, and smoke test"
```

---

## Task 3: UserControllerIT

Reference implementation; covers every `/users` endpoint plus the auth-required path. Note on the unauthenticated assertion: Spring Security's anonymous handling returns 403 (or 401 depending on the entry point) — assert `is4xxClientError()` so the test is robust to that detail while still proving the request was rejected.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/user/UserControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest` (mockMvc, objectMapper, `authHeaderFor`), `UserRepository`, `User`, `UserInputDto`.
- Endpoints covered: `GET /users` (paged), `GET /users/all`, `GET /users/{id}` (found + 404), `GET /users/by-firebase-uid`, `GET /users/by-email` (found + 404), `GET /users/me`, `POST /users`, `DELETE /users/{id}` (found + 404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.user;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.dto.UserInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User persistUser(final String uid, final String email, final String first, final String last) {
        return userRepository.save(User.builder()
                .firebaseUid(uid)
                .email(email)
                .firstName(first)
                .lastName(last)
                .build());
    }

    @Test
    void getAllUsers_returnsPersistedUsers() throws Exception {
        persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users/all")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@example.com"));
    }

    @Test
    void getAllUsers_paged_returnsContentArray() throws Exception {
        persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getUserById_found() throws Exception {
        final User user = persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users/{id}", user.getId())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getUserByFirebaseUid_found() throws Exception {
        persistUser("uid-find", "find@example.com", "Bob", "B");

        mockMvc.perform(get("/users/by-firebase-uid")
                        .param("firebaseUid", "uid-find")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("find@example.com"));
    }

    @Test
    void getUserByEmail_found() throws Exception {
        persistUser("uid-mail", "byemail@example.com", "Carol", "C");

        mockMvc.perform(get("/users/by-email")
                        .param("email", "byemail@example.com")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Carol"));
    }

    @Test
    void getUserByEmail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .param("email", "missing@example.com")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentUser_resolvesAuthenticatedUid() throws Exception {
        persistUser("uid-me", "me@example.com", "Mia", "M");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", authHeaderFor("uid-me", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("uid-me"));
    }

    @Test
    void createUser_persistsAndReturnsDto() throws Exception {
        final UserInputDto input = new UserInputDto("uid-new", "new@example.com", "Ned", "N");

        mockMvc.perform(post("/users")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        assertThat(userRepository.findByFirebaseUid("uid-new")).isPresent();
    }

    @Test
    void deleteUser_removesRow() throws Exception {
        final User user = persistUser("uid-del", "del@example.com", "Dan", "D");

        mockMvc.perform(delete("/users/{id}", user.getId())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/users/all"))
                .andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run the test class**

Run: `./mvnw -q -Dit.test=UserControllerIT verify`
Expected: PASS (all methods green). A 404 assertion failing on a "found" case, or a 200 failing, indicates a wiring problem to diagnose.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/user/UserControllerIT.java
git commit -m "test: add UserController integration tests"
```

---

## Task 4: VideoControllerIT

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/video/VideoControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `VideoRepository`, `Video`, `VideoInputDto`.
- Endpoints: `GET /videos` (paged), `GET /videos/all`, `GET /videos/{id}` (found + 404), `POST /videos`, `DELETE /videos/{id}` (404), `DELETE /videos` (delete all).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.video;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.video.dto.VideoInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VideoControllerIT extends AbstractIntegrationTest {

    @Autowired
    private VideoRepository videoRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-v", "v@example.com");
    }

    private Video persistVideo(final String title) {
        return videoRepository.save(Video.builder()
                .title(title)
                .url("https://youtu.be/" + title)
                .thumbnailUrl("https://img/" + title)
                .build());
    }

    @Test
    void getAllVideos_returnsPersisted() throws Exception {
        persistVideo("clip");

        mockMvc.perform(get("/videos/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("clip"));
    }

    @Test
    void getAllVideos_paged() throws Exception {
        persistVideo("clip");

        mockMvc.perform(get("/videos").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getVideoById_found() throws Exception {
        final Video video = persistVideo("clip");

        mockMvc.perform(get("/videos/{id}", video.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("clip"));
    }

    @Test
    void getVideoById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/videos/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVideo_persists() throws Exception {
        final VideoInputDto input = new VideoInputDto("new", "https://youtu.be/new", "https://img/new");

        mockMvc.perform(post("/videos")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("new"));

        assertThat(videoRepository.findAll()).extracting(Video::getTitle).contains("new");
    }

    @Test
    void deleteVideo_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/videos/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllVideos_emptiesTable() throws Exception {
        persistVideo("a");
        persistVideo("b");

        mockMvc.perform(delete("/videos").header("Authorization", auth()))
                .andExpect(status().isOk());

        assertThat(videoRepository.count()).isZero();
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/videos/all")).andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=VideoControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/video/VideoControllerIT.java
git commit -m "test: add VideoController integration tests"
```

---

## Task 5: ChatControllerIT

Covers participant resolution (all-present vs partially-missing → `ChatParticipantsNotFoundException` → 404) and the `chat_participants` join table.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/chat/ChatControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `ChatRepository`, `UserRepository`, `User`, `ChatInputDto`.
- Endpoints: `GET /chats` (paged), `GET /chats/all`, `GET /chats/{id}` (found + 404), `POST /chats` (success + participants-not-found 404), `PATCH /chats/{id}/participants`, `DELETE /chats/{id}` (404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-c", "c@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Chat persistChatWith(final User... participants) {
        return chatRepository.save(Chat.builder()
                .participants(List.of(participants))
                .build());
    }

    @Test
    void createChat_withExistingParticipants_persistsJoinRows() throws Exception {
        final User u1 = persistUser("p1");
        final User u2 = persistUser("p2");
        final ChatInputDto input = new ChatInputDto(List.of(u1.getId(), u2.getId()));

        mockMvc.perform(post("/chats")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void createChat_withMissingParticipant_returns404() throws Exception {
        final User u1 = persistUser("p1");
        final ChatInputDto input = new ChatInputDto(List.of(u1.getId(), UUID.randomUUID()));

        mockMvc.perform(post("/chats")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatById_found() throws Exception {
        final Chat chat = persistChatWith(persistUser("g1"));

        mockMvc.perform(get("/chats/{id}", chat.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chat.getId().toString()));
    }

    @Test
    void getChatById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/chats/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllChats_paged() throws Exception {
        mockMvc.perform(get("/chats").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllChats_list() throws Exception {
        persistChatWith(persistUser("l1"));

        mockMvc.perform(get("/chats/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void addParticipants_appendsToChat() throws Exception {
        final Chat chat = persistChatWith(persistUser("a1"));
        final User added = persistUser("a2");

        mockMvc.perform(patch("/chats/{id}/participants", chat.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(added.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void deleteChat_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/chats/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/chats/all")).andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=ChatControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/chat/ChatControllerIT.java
git commit -m "test: add ChatController integration tests"
```

---

## Task 6: MessageControllerIT

Verifies the WebSocket broadcast and FCM fan-out via the mocked `SimpMessagingTemplate` and `FcmService`, plus chat-not-found and sender-not-found paths.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/message/MessageControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest` (incl. mocked `messagingTemplate` + `fcmService`), `MessageRepository`, `ChatRepository`, `UserRepository`, `DeviceTokenRepository`, `Chat`, `User`, `DeviceToken`, `Message`, `MessageInputDto`, `DeliveryStatus`.
- Endpoints: `GET /messages` (paged), `GET /messages/all`, `GET /messages/{id}` (found + 404), `GET /messages/chat/{chatId}`, `GET /messages/sender/{senderId}`, `POST /messages` (success + broadcast/FCM verify + chat-404 + sender-404), `DELETE /messages/{id}` (404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.Chat;
import iliev.yt.share.backend.chat.ChatRepository;
import iliev.yt.share.backend.devicetoken.DeviceToken;
import iliev.yt.share.backend.devicetoken.DeviceTokenRepository;
import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-m", "m@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Chat persistChat(final User... participants) {
        return chatRepository.save(Chat.builder()
                .participants(List.of(participants))
                .build());
    }

    private Message persistMessage(final Chat chat, final User sender, final String content) {
        return messageRepository.save(Message.builder()
                .content(content)
                .status(DeliveryStatus.SENT)
                .chat(chat)
                .sender(sender)
                .build());
    }

    @Test
    void createMessage_persists_broadcasts_andNotifiesRecipientTokens() throws Exception {
        final User sender = persistUser("sender");
        final User recipient = persistUser("recipient");
        final Chat chat = persistChat(sender, recipient);
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-recipient")
                .platform("ANDROID")
                .user(recipient)
                .build());

        final MessageInputDto input = new MessageInputDto("hello", DeliveryStatus.SENT, chat.getId(), sender.getId());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("hello"));

        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), (Object) any());
        verify(fcmService).sendPushNotification(
                eq("fcm-recipient"), anyString(), eq("hello"),
                eq(chat.getId().toString()), eq(sender.getId().toString()), anyString());
    }

    @Test
    void createMessage_chatNotFound_returns404() throws Exception {
        final User sender = persistUser("sender2");
        final MessageInputDto input = new MessageInputDto("x", DeliveryStatus.SENT, UUID.randomUUID(), sender.getId());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMessage_senderNotFound_returns404() throws Exception {
        final Chat chat = persistChat(persistUser("only"));
        final MessageInputDto input = new MessageInputDto("x", DeliveryStatus.SENT, chat.getId(), UUID.randomUUID());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessageById_found() throws Exception {
        final User sender = persistUser("s3");
        final Chat chat = persistChat(sender);
        final Message message = persistMessage(chat, sender, "stored");

        mockMvc.perform(get("/messages/{id}", message.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("stored"));
    }

    @Test
    void getMessageById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/messages/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessagesByChatId_returnsChatMessages() throws Exception {
        final User sender = persistUser("s4");
        final Chat chat = persistChat(sender);
        persistMessage(chat, sender, "c1");

        mockMvc.perform(get("/messages/chat/{chatId}", chat.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("c1"));
    }

    @Test
    void getMessagesBySenderId_returnsSenderMessages() throws Exception {
        final User sender = persistUser("s5");
        final Chat chat = persistChat(sender);
        persistMessage(chat, sender, "s1");

        mockMvc.perform(get("/messages/sender/{senderId}", sender.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("s1"));
    }

    @Test
    void getAllMessages_paged() throws Exception {
        mockMvc.perform(get("/messages").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllMessages_list() throws Exception {
        mockMvc.perform(get("/messages/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteMessage_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/messages/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/messages/all")).andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=MessageControllerIT verify`
Expected: PASS, including the `messagingTemplate` and `fcmService` verifications.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/message/MessageControllerIT.java
git commit -m "test: add MessageController integration tests"
```

---

## Task 7: FriendshipControllerIT

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/friends/FriendshipControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `FriendshipRepository`, `UserRepository`, `Friendship`, `User`, `FriendshipInputDto`, `FriendshipStatus`.
- Endpoints: `GET /friendships` (paged), `GET /friendships/all`, `GET /friendships/{id}` (found + 404), `GET /friendships/user/{userId}`, `GET /friendships/user/{userId}/status`, `POST /friendships`, `PATCH /friendships/{id}/status` (success + 404), `DELETE /friendships/{id}` (404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendshipControllerIT extends AbstractIntegrationTest {

    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-f", "f@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Friendship persistFriendship(final User user, final User friend, final FriendshipStatus status) {
        return friendshipRepository.save(Friendship.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .build());
    }

    @Test
    void createFriendship_persists() throws Exception {
        final User user = persistUser("u1");
        final User friend = persistUser("u2");
        final FriendshipInputDto input = new FriendshipInputDto(user.getId(), friend.getId(), FriendshipStatus.PENDING);

        mockMvc.perform(post("/friendships")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getFriendshipById_found() throws Exception {
        final Friendship f = persistFriendship(persistUser("a"), persistUser("b"), FriendshipStatus.PENDING);

        mockMvc.perform(get("/friendships/{id}", f.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.getId().toString()));
    }

    @Test
    void getFriendshipById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/friendships/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFriendshipsByUserId_returnsList() throws Exception {
        final User user = persistUser("byu");
        persistFriendship(user, persistUser("byf"), FriendshipStatus.PENDING);

        mockMvc.perform(get("/friendships/user/{userId}", user.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFriendshipsByUserIdAndStatus_returnsList() throws Exception {
        final User user = persistUser("su");
        persistFriendship(user, persistUser("sf"), FriendshipStatus.ACCEPTED);

        mockMvc.perform(get("/friendships/user/{userId}/status", user.getId())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllFriendships_paged() throws Exception {
        mockMvc.perform(get("/friendships").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllFriendships_list() throws Exception {
        mockMvc.perform(get("/friendships/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateFriendshipStatus_changesStatus() throws Exception {
        final Friendship f = persistFriendship(persistUser("up1"), persistUser("up2"), FriendshipStatus.PENDING);

        mockMvc.perform(patch("/friendships/{id}/status", f.getId())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void updateFriendshipStatus_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/friendships/{id}/status", UUID.randomUUID())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFriendship_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/friendships/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/friendships/all")).andExpect(status().is4xxClientError());
    }
}
```

**Note:** `FriendshipStatus` constant names (`PENDING`, `ACCEPTED`) are used above. Before running, open `backend/src/main/java/iliev/yt/share/backend/friends/enums/FriendshipStatus.java` and confirm those names; if they differ, substitute the actual constants in the test.

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=FriendshipControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/friends/FriendshipControllerIT.java
git commit -m "test: add FriendshipController integration tests"
```

---

## Task 8: UserPreferencesControllerIT

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/user/preferences/UserPreferencesControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `UserPreferencesRepository`, `UserRepository`, `UserPreferences`, `User`, `UserPreferencesInputDto`.
- Endpoints: `GET /user-preferences` (paged), `GET /user-preferences/all`, `GET /user-preferences/{id}` (found + 404), `GET /user-preferences/user/{userId}` (found + 404), `POST /user-preferences` (success + user-404), `PUT /user-preferences/{id}` (success + 404), `DELETE /user-preferences/{id}` (404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPreferencesControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-pref", "pref@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private UserPreferences persistPrefs(final User user, final boolean dark) {
        return userPreferencesRepository.save(UserPreferences.builder()
                .darkMode(dark)
                .notificationsEnabled(true)
                .trackingEnabled(false)
                .user(user)
                .build());
    }

    @Test
    void createUserPreferences_persists() throws Exception {
        final User user = persistUser("c1");
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, true, false, user.getId());

        mockMvc.perform(post("/user-preferences")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void createUserPreferences_userNotFound_returns404() throws Exception {
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, true, false, UUID.randomUUID());

        mockMvc.perform(post("/user-preferences")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_found() throws Exception {
        final UserPreferences prefs = persistPrefs(persistUser("g1"), true);

        mockMvc.perform(get("/user-preferences/{id}", prefs.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prefs.getId().toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/user-preferences/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUserId_found() throws Exception {
        final User user = persistUser("byuser");
        persistPrefs(user, false);

        mockMvc.perform(get("/user-preferences/user/{userId}", user.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void getByUserId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/user-preferences/user/{userId}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_paged() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_list() throws Exception {
        mockMvc.perform(get("/user-preferences/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updatePreferences_changesValues() throws Exception {
        final User user = persistUser("up");
        final UserPreferences prefs = persistPrefs(user, false);
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, false, true, user.getId());

        mockMvc.perform(put("/user-preferences/{id}", prefs.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true));
    }

    @Test
    void updatePreferences_notFound_returns404() throws Exception {
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, false, true, UUID.randomUUID());

        mockMvc.perform(put("/user-preferences/{id}", UUID.randomUUID())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePreferences_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/user-preferences/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/user-preferences/all")).andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=UserPreferencesControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/user/preferences/UserPreferencesControllerIT.java
git commit -m "test: add UserPreferencesController integration tests"
```

---

## Task 9: DeviceControllerIT

A `Device` is created through an existing `UserPreferences` (whose creation needs a `User`). Build that chain in the fixtures.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/device/DeviceControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `DeviceRepository`, `UserPreferencesRepository`, `UserRepository`, `Device`, `UserPreferences`, `User`, `DeviceInputDto`.
- Endpoints: `GET /devices` (paged), `GET /devices/all`, `GET /devices/{id}` (found + 404), `GET /devices/user-preferences/{userPreferencesId}`, `POST /devices` (success + prefs-404), `PUT /devices/{id}` (success + 404), `DELETE /devices/{id}` (404).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import iliev.yt.share.backend.user.preferences.UserPreferencesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-dev", "dev@example.com");
    }

    private UserPreferences persistPrefs(final String suffix) {
        final User user = userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
        return userPreferencesRepository.save(UserPreferences.builder()
                .darkMode(false)
                .notificationsEnabled(true)
                .trackingEnabled(false)
                .user(user)
                .build());
    }

    private Device persistDevice(final UserPreferences prefs, final String host) {
        return deviceRepository.save(Device.builder()
                .hostName(host)
                .ipAddress("10.0.0.1")
                .port("8080")
                .lastConnectedTo(LocalDateTime.now())
                .userPreferences(prefs)
                .build());
    }

    @Test
    void createDevice_persists() throws Exception {
        final UserPreferences prefs = persistPrefs("c1");
        final DeviceInputDto input = new DeviceInputDto("laptop", "10.0.0.5", "9090", LocalDateTime.now(), prefs.getId());

        mockMvc.perform(post("/devices")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("laptop"))
                .andExpect(jsonPath("$.userPreferencesId").value(prefs.getId().toString()));
    }

    @Test
    void createDevice_prefsNotFound_returns404() throws Exception {
        final DeviceInputDto input = new DeviceInputDto("laptop", "10.0.0.5", "9090", LocalDateTime.now(), UUID.randomUUID());

        mockMvc.perform(post("/devices")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeviceById_found() throws Exception {
        final Device device = persistDevice(persistPrefs("g1"), "host-g1");

        mockMvc.perform(get("/devices/{id}", device.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("host-g1"));
    }

    @Test
    void getDeviceById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/devices/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDevicesByUserPreferencesId_returnsList() throws Exception {
        final UserPreferences prefs = persistPrefs("byp");
        persistDevice(prefs, "host-byp");

        mockMvc.perform(get("/devices/user-preferences/{userPreferencesId}", prefs.getId())
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostName").value("host-byp"));
    }

    @Test
    void getAllDevices_paged() throws Exception {
        mockMvc.perform(get("/devices").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllDevices_list() throws Exception {
        mockMvc.perform(get("/devices/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateDevice_changesValues() throws Exception {
        final UserPreferences prefs = persistPrefs("up");
        final Device device = persistDevice(prefs, "old-host");
        final DeviceInputDto input = new DeviceInputDto("new-host", "10.0.0.9", "7070", LocalDateTime.now(), prefs.getId());

        mockMvc.perform(put("/devices/{id}", device.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("new-host"));
    }

    @Test
    void deleteDevice_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/devices/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/devices/all")).andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=DeviceControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/device/DeviceControllerIT.java
git commit -m "test: add DeviceController integration tests"
```

---

## Task 10: DeviceTokenControllerIT

Both endpoints resolve the current user from the authenticated Firebase uid via `SecurityUtils`, so the authenticated uid MUST match a persisted user.

**Files:**
- Create: `backend/src/test/java/iliev/yt/share/backend/devicetoken/DeviceTokenControllerIT.java`

**Interfaces:**
- Consumes: `AbstractIntegrationTest`, `DeviceTokenRepository`, `UserRepository`, `DeviceToken`, `User`, `DeviceTokenInputDto`.
- Endpoints: `POST /device-tokens` (register: new + upsert existing token; user-of-uid not found → 404), `DELETE /device-tokens` (removes current user's tokens).

- [ ] **Step 1: Write the test class**

```java
package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceTokenControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserRepository userRepository;

    private User persistUser(final String uid, final String email) {
        return userRepository.save(User.builder()
                .firebaseUid(uid)
                .email(email)
                .firstName("F")
                .lastName("L")
                .build());
    }

    @Test
    void registerToken_createsNewToken() throws Exception {
        persistUser("uid-reg", "reg@example.com");
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-abc", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-reg", "reg@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fcmToken").value("fcm-abc"))
                .andExpect(jsonPath("$.platform").value("ANDROID"));

        assertThat(deviceTokenRepository.findByFcmToken("fcm-abc")).isPresent();
    }

    @Test
    void registerToken_existingToken_isReassignedNotDuplicated() throws Exception {
        final User user = persistUser("uid-up", "up@example.com");
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-shared")
                .platform("IOS")
                .user(user)
                .build());
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-shared", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-up", "up@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("ANDROID"));

        assertThat(deviceTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void registerToken_noUserForUid_returns404() throws Exception {
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-x", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-missing", "missing@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeToken_deletesCurrentUsersTokens() throws Exception {
        final User user = persistUser("uid-del", "del@example.com");
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-del")
                .platform("ANDROID")
                .user(user)
                .build());

        mockMvc.perform(delete("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-del", "del@example.com")))
                .andExpect(status().isOk());

        assertThat(deviceTokenRepository.findByFcmToken("fcm-del")).isEmpty();
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(delete("/device-tokens")).andExpect(status().is4xxClientError());
    }
}
```

**Note:** This test calls `deviceTokenRepository.findByFcmToken(...)` and `findByUserId(...)`-backed behavior that already exists (`DeviceTokenService` uses them). `findByFcmToken` returns `Optional<DeviceToken>` — confirmed by the service. No new repository methods are required.

- [ ] **Step 2: Run**

Run: `./mvnw -q -Dit.test=DeviceTokenControllerIT verify`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/iliev/yt/share/backend/devicetoken/DeviceTokenControllerIT.java
git commit -m "test: add DeviceTokenController integration tests"
```

---

## Task 11: Full-suite verification

Confirms every IT runs together against the shared container and the unit suite is unaffected.

**Files:** none (verification only).

- [ ] **Step 1: Run the entire build**

Run: `./mvnw -q clean verify`
Expected: BUILD SUCCESS. Surefire runs the existing `*Test` unit tests; Failsafe runs all eight `*ControllerIT` classes plus `InfrastructureIT`, each green, sharing one Postgres container.

- [ ] **Step 2: Confirm `test` still excludes ITs**

Run: `./mvnw -q test`
Expected: BUILD SUCCESS, and the console shows only `*Test`/`*Tests` classes executing (no `*IT`). Confirms Surefire/Failsafe separation.

- [ ] **Step 3: Commit (only if any fixups were needed)**

```bash
git add -A
git commit -m "test: finalize backend integration test suite"
```

---

## Self-Review

**Spec coverage:**
- Boundary (security filter → controller → service → mapper → repo → real Postgres): Tasks 2–10. ✔
- Mocked boundaries `FirebaseAuth`/`FcmService`/`SimpMessagingTemplate`: Task 2 base class; FCM + WebSocket verified in Task 6. ✔
- Testcontainers Postgres + real Liquibase + `validate`: Tasks 1–2. ✔
- One IT per controller (8): Tasks 3–10. ✔
- `FcmService` excluded from HTTP IT (mocked, keeps existing unit test): honored — no `FcmService` IT. ✔
- Production change = single `@ConditionalOnProperty` on `FirebaseConfig`: Task 1, Step 3. ✔
- Failsafe `*IT` naming, `mvn verify` runs them, `mvn test` unit-only: Tasks 1 & 11. ✔
- Per-endpoint assertions (happy path + 404 + auth-required): present in each IT; validation→400 correctly omitted (no Bean Validation exists). ✔
- Domain-specifics: Message broadcast/FCM (Task 6), Chat participants + join table (Task 5), getCurrentUser via auth uid (Task 3). ✔
- Existing `*ServiceTest` untouched: no task modifies them. ✔

**Placeholder scan:** No TBD/TODO/"similar to"/"add error handling" — every step has concrete code or an exact command. Two explicit "confirm the constant/method name before running" notes (FriendshipStatus in Task 7, repository method in Task 10) are verification guards, not placeholders.

**Type consistency:** `authHeaderFor(String, String)` defined in Task 2 and used identically everywhere. Mocked bean field names (`firebaseAuth`, `fcmService`, `messagingTemplate`) consistent between base class and Task 6 verifications. DTO constructor arities match the records read from source (`UserInputDto`/`VideoInputDto`/`ChatInputDto`/`MessageInputDto`/`FriendshipInputDto`/`UserPreferencesInputDto`/`DeviceInputDto`/`DeviceTokenInputDto`). Endpoint paths match the controllers verbatim.
