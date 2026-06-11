# Android Chat Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a chat feature to the Android app with friends management, real-time messaging via WebSocket/STOMP, and FCM push notifications — including backend WebSocket support and frontend migration from polling to WebSocket.

**Architecture:** Three-phase approach: (1) Backend additions — WebSocket broker, device-token domain, user/me endpoint, FCM push sending, add `createdAt` to `MessageOutputDto`; (2) Frontend — replace 3-second polling with STOMP subscription; (3) Android — Ktor API services, Krossbow STOMP client, Compose UI screens, FCM service, Koin wiring, bottom-nav update.

**Tech Stack:** Spring Boot 4 + WebSocket/STOMP, Angular 20 + @stomp/stompjs, Kotlin + Jetpack Compose + Ktor + Krossbow + Firebase (Auth + FCM) + Koin

---

## File Map

### Backend — New Files
- `backend/pom.xml` — add `spring-boot-starter-websocket` dependency
- `backend/src/main/java/iliev/yt/share/backend/websocket/WebSocketConfig.java` — STOMP broker config
- `backend/src/main/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptor.java` — Firebase token auth on CONNECT
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceToken.java` — entity
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenRepository.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenService.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenController.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenMapper.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/dto/DeviceTokenInputDto.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/dto/DeviceTokenOutputDto.java`
- `backend/src/main/java/iliev/yt/share/backend/devicetoken/exception/DeviceTokenNotFoundException.java`
- `backend/src/main/java/iliev/yt/share/backend/notification/FcmService.java` — sends push notifications
- `backend/src/main/resources/db/changelog/31-05-changelog.yaml` — device_tokens table

### Backend — Modified Files
- `backend/src/main/java/iliev/yt/share/backend/message/dto/MessageOutputDto.java` — add `createdAt`
- `backend/src/main/java/iliev/yt/share/backend/message/MessageService.java` — broadcast via STOMP + send FCM
- `backend/src/main/java/iliev/yt/share/backend/security/SecurityConfig.java` — permit WebSocket endpoints
- `backend/src/main/java/iliev/yt/share/backend/user/UserController.java` — add `GET /users/me`
- `backend/src/main/java/iliev/yt/share/backend/user/UserService.java` — add `getCurrentUser()`
- `backend/src/main/java/iliev/yt/share/backend/common/enums/ExceptionMessages.java` — add DEVICE_TOKEN_NOT_FOUND
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml` — include new changelog

### Frontend — New Files
- `frontend/src/app/core/services/websocket.service.ts` — STOMP client service

### Frontend — Modified Files
- `frontend/package.json` — add `@stomp/stompjs`
- `frontend/src/environments/environment.ts` — add `wsUrl`
- `frontend/src/environments/environment.prod.ts` — add `wsUrl`
- `frontend/src/app/features/chat/chat-page.component.ts` — replace polling with WebSocket subscription

### Android — New Files
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/UserOutputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/FriendshipOutputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/FriendshipInputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/ChatOutputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/ChatInputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/MessageOutputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/MessageInputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/DeviceTokenInputDto.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/FriendshipApiService.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/ChatApiService.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/MessageApiService.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/UserApiService.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/DeviceTokenApiService.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/StompSessionManager.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/ChatRepository.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsViewModel.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsScreen.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationViewModel.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationScreen.kt`
- `YTShare.Android/app/src/main/java/com/example/ytshare/fcm/YTShareFirebaseMessagingService.kt`

### Android — Modified Files
- `YTShare.Android/app/build.gradle.kts` — add Krossbow + FCM dependencies
- `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt` — register new services
- `YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt` — add Chat bottom nav tab + conversation route
- `YTShare.Android/app/src/main/AndroidManifest.xml` — register FCM service

---

### Task 1: Backend — Add WebSocket dependency and MessageOutputDto.createdAt

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/iliev/yt/share/backend/message/dto/MessageOutputDto.java`

- [ ] **Step 1: Add spring-boot-starter-websocket to pom.xml**

Add after the `spring-boot-starter-webmvc` dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 2: Add createdAt to MessageOutputDto**

Replace the record in `MessageOutputDto.java`:

```java
package iliev.yt.share.backend.message.dto;

import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import java.util.Date;
import java.util.UUID;

public record MessageOutputDto(
        UUID id,
        String content,
        DeliveryStatus status,
        ChatOutputDto chat,
        UserOutputDto sender,
        Date createdAt
) {
}
```

MapStruct will automatically map `createdAt` from `BaseEntity` since the field name matches.

- [ ] **Step 3: Verify the backend compiles**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml backend/src/main/java/iliev/yt/share/backend/message/dto/MessageOutputDto.java
git commit -m "feat: add websocket dependency and createdAt to MessageOutputDto"
```

---

### Task 2: Backend — WebSocket config and Firebase auth interceptor

**Files:**
- Create: `backend/src/main/java/iliev/yt/share/backend/websocket/WebSocketConfig.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/websocket/WebSocketAuthInterceptor.java`
- Modify: `backend/src/main/java/iliev/yt/share/backend/security/SecurityConfig.java`

- [ ] **Step 1: Create WebSocketConfig.java**

```java
package iliev.yt.share.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
```

- [ ] **Step 2: Create WebSocketAuthInterceptor.java**

```java
package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import iliev.yt.share.backend.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            final String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                final String token = authHeader.substring(BEARER_PREFIX.length());
                try {
                    final FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token);
                    final FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                            firebaseToken,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    accessor.setUser(authentication);
                    log.debug("WebSocket authenticated user: {}", firebaseToken.getUid());
                } catch (Exception e) {
                    log.warn("WebSocket Firebase token verification failed: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid Firebase token");
                }
            } else {
                throw new IllegalArgumentException("Missing Authorization header");
            }
        }

        return message;
    }
}
```

- [ ] **Step 3: Update SecurityConfig to permit WebSocket endpoints**

In `SecurityConfig.java`, update the `authorizeHttpRequests` block to add WebSocket paths:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**", "/actuator/health", "/ws/**").permitAll()
        .anyRequest().authenticated()
)
```

- [ ] **Step 4: Verify the backend compiles**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/websocket/ backend/src/main/java/iliev/yt/share/backend/security/SecurityConfig.java
git commit -m "feat: add WebSocket/STOMP config with Firebase auth interceptor"
```

---

### Task 3: Backend — GET /users/me endpoint

**Files:**
- Modify: `backend/src/main/java/iliev/yt/share/backend/user/UserController.java`
- Modify: `backend/src/main/java/iliev/yt/share/backend/user/UserService.java`

- [ ] **Step 1: Add getCurrentUser() to UserService**

Add this method to `UserService.java`:

```java
public UserOutputDto getCurrentUser() {
    final String firebaseUid = SecurityUtils.requireCurrentUserUid();
    final User user = userRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow(() -> new UserNotFoundException(firebaseUid));
    return userMapper.toOutputDto(user);
}
```

Add the import:

```java
import iliev.yt.share.backend.security.SecurityUtils;
```

- [ ] **Step 2: Add GET /users/me to UserController**

Add this method to `UserController.java`:

```java
@GetMapping("/me")
public UserOutputDto getCurrentUser() {
    return userService.getCurrentUser();
}
```

- [ ] **Step 3: Verify the backend compiles**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/user/UserController.java backend/src/main/java/iliev/yt/share/backend/user/UserService.java
git commit -m "feat: add GET /users/me endpoint for authenticated user lookup"
```

---

### Task 4: Backend — DeviceToken domain and Liquibase migration

**Files:**
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceToken.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenRepository.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenService.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenController.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/DeviceTokenMapper.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/dto/DeviceTokenInputDto.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/dto/DeviceTokenOutputDto.java`
- Create: `backend/src/main/java/iliev/yt/share/backend/devicetoken/exception/DeviceTokenNotFoundException.java`
- Create: `backend/src/main/resources/db/changelog/31-05-changelog.yaml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- Modify: `backend/src/main/java/iliev/yt/share/backend/common/enums/ExceptionMessages.java`

- [ ] **Step 1: Create Liquibase changelog**

Create `backend/src/main/resources/db/changelog/31-05-changelog.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: 1748700000000-1
      author: iliev
      objectQuotingStrategy: QUOTE_ONLY_RESERVED_WORDS
      changes:
        - createTable:
            tableName: device_tokens
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: user_id
                  type: UUID
                  constraints:
                    nullable: false
              - column:
                  name: fcm_token
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: platform
                  type: VARCHAR(32)
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
              - column:
                  name: updated_at
                  type: TIMESTAMP
        - addForeignKeyConstraint:
            baseTableName: device_tokens
            baseColumnNames: user_id
            referencedTableName: users
            referencedColumnNames: id
            constraintName: fk_device_tokens_user
```

- [ ] **Step 2: Add changelog to master**

In `db.changelog-master.yaml`, add at the end:

```yaml
  - include:
      file: db/changelog/31-05-changelog.yaml
```

- [ ] **Step 3: Create DeviceToken entity**

```java
package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {
    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

- [ ] **Step 4: Create DeviceTokenRepository**

```java
package iliev.yt.share.backend.devicetoken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByFcmToken(String fcmToken);
    List<DeviceToken> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
```

- [ ] **Step 5: Create DTOs**

`DeviceTokenInputDto.java`:

```java
package iliev.yt.share.backend.devicetoken.dto;

public record DeviceTokenInputDto(
        String fcmToken,
        String platform
) {
}
```

`DeviceTokenOutputDto.java`:

```java
package iliev.yt.share.backend.devicetoken.dto;

import java.util.UUID;

public record DeviceTokenOutputDto(
        UUID id,
        String fcmToken,
        String platform
) {
}
```

- [ ] **Step 6: Create DeviceTokenMapper**

```java
package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import org.mapstruct.Mapper;

@Mapper
public interface DeviceTokenMapper {
    DeviceTokenOutputDto toOutputDto(final DeviceToken deviceToken);
}
```

- [ ] **Step 7: Create DeviceTokenNotFoundException**

```java
package iliev.yt.share.backend.devicetoken.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;
import java.util.UUID;
import static iliev.yt.share.backend.common.enums.ExceptionMessages.DEVICE_TOKEN_NOT_FOUND;

public class DeviceTokenNotFoundException extends GenericNotFoundException {
    public DeviceTokenNotFoundException(UUID id) {
        super(DEVICE_TOKEN_NOT_FOUND.getMessage(id));
    }
}
```

Note: `DEVICE_TOKEN_NOT_FOUND` already exists in `ExceptionMessages.java`.

- [ ] **Step 8: Create DeviceTokenService**

```java
package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import iliev.yt.share.backend.security.SecurityUtils;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;
    private final UserRepository userRepository;

    @Transactional
    public DeviceTokenOutputDto registerToken(final DeviceTokenInputDto inputDto) {
        final String firebaseUid = SecurityUtils.requireCurrentUserUid();
        final User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));

        final DeviceToken deviceToken = deviceTokenRepository.findByFcmToken(inputDto.fcmToken())
                .map(existing -> {
                    existing.setUser(user);
                    existing.setPlatform(inputDto.platform());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .fcmToken(inputDto.fcmToken())
                        .platform(inputDto.platform())
                        .user(user)
                        .build());

        final DeviceToken saved = deviceTokenRepository.save(deviceToken);
        return deviceTokenMapper.toOutputDto(saved);
    }

    @Transactional
    public void removeTokensForCurrentUser() {
        final String firebaseUid = SecurityUtils.requireCurrentUserUid();
        final User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));
        deviceTokenRepository.deleteByUserId(user.getId());
    }

    public List<DeviceToken> getTokensByUserIds(final List<UUID> userIds) {
        return userIds.stream()
                .flatMap(id -> deviceTokenRepository.findByUserId(id).stream())
                .toList();
    }
}
```

- [ ] **Step 9: Create DeviceTokenController**

```java
package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public DeviceTokenOutputDto registerToken(@RequestBody final DeviceTokenInputDto inputDto) {
        return deviceTokenService.registerToken(inputDto);
    }

    @DeleteMapping
    public void removeToken() {
        deviceTokenService.removeTokensForCurrentUser();
    }
}
```

- [ ] **Step 10: Verify the backend compiles**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/devicetoken/ backend/src/main/resources/db/changelog/
git commit -m "feat: add DeviceToken domain for FCM token registration"
```

---

### Task 5: Backend — FCM push notification service and STOMP broadcast

**Files:**
- Create: `backend/src/main/java/iliev/yt/share/backend/notification/FcmService.java`
- Modify: `backend/src/main/java/iliev/yt/share/backend/message/MessageService.java`

- [ ] **Step 1: Create FcmService**

```java
package iliev.yt.share.backend.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendPushNotification(final String fcmToken, final String title, final String body,
                                     final String chatId, final String senderId, final String senderName) {
        final Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body.length() > 100 ? body.substring(0, 100) + "..." : body)
                        .build())
                .putData("chatId", chatId)
                .putData("senderId", senderId)
                .putData("senderName", senderName)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("Failed to send FCM notification to token {}: {}", fcmToken, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Update MessageService to broadcast via STOMP and send FCM**

Add new fields and update `createMessage()` in `MessageService.java`:

Add these fields to the class:

```java
private final SimpMessagingTemplate messagingTemplate;
private final DeviceTokenService deviceTokenService;
private final FcmService fcmService;
```

Add these imports:

```java
import iliev.yt.share.backend.devicetoken.DeviceToken;
import iliev.yt.share.backend.devicetoken.DeviceTokenService;
import iliev.yt.share.backend.notification.FcmService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
```

Replace the `createMessage()` method:

```java
@Transactional
public MessageOutputDto createMessage(final MessageInputDto inputDto) {
    final Chat chat = chatRepository.findById(inputDto.chatId())
            .orElseThrow(() -> new ChatNotFoundException(inputDto.chatId()));
    final User sender = userRepository.findById(inputDto.senderId())
            .orElseThrow(() -> new UserNotFoundException(inputDto.senderId()));

    final Message message = Message.builder()
            .content(inputDto.content())
            .status(inputDto.status())
            .chat(chat)
            .sender(sender)
            .build();

    final Message savedMessage = messageRepository.save(message);
    final MessageOutputDto outputDto = messageMapper.toOutputDto(savedMessage);

    messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), outputDto);

    final List<UUID> recipientIds = chat.getParticipants().stream()
            .map(User::getId)
            .filter(id -> !id.equals(sender.getId()))
            .toList();
    final List<DeviceToken> tokens = deviceTokenService.getTokensByUserIds(recipientIds);
    final String senderName = sender.getFirstName();
    for (final DeviceToken token : tokens) {
        fcmService.sendPushNotification(
                token.getFcmToken(),
                senderName,
                inputDto.content(),
                chat.getId().toString(),
                sender.getId().toString(),
                senderName
        );
    }

    return outputDto;
}
```

- [ ] **Step 3: Verify the backend compiles**

Run: `cd backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/notification/ backend/src/main/java/iliev/yt/share/backend/message/MessageService.java
git commit -m "feat: broadcast messages via STOMP and send FCM push notifications"
```

---

### Task 6: Frontend — Replace polling with WebSocket/STOMP

**Files:**
- Create: `frontend/src/app/core/services/websocket.service.ts`
- Modify: `frontend/src/environments/environment.ts`
- Modify: `frontend/src/environments/environment.prod.ts`
- Modify: `frontend/src/app/features/chat/chat-page.component.ts`

- [ ] **Step 1: Install @stomp/stompjs**

Run: `cd frontend && npm install @stomp/stompjs`

- [ ] **Step 2: Add wsUrl to environment files**

In `environment.ts`, add `wsUrl`:

```typescript
export const environment = {
  production: false,
  firebase: {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.firebasestorage.app",
    messagingSenderId: "YOUR_SENDER_ID",
    appId: "YOUR_APP_ID"
  },
  apiUrl: 'http://localhost:8080',
  wsUrl: 'http://localhost:8080/ws'
};
```

Do the same for `environment.prod.ts` with the production URLs.

- [ ] **Step 3: Create WebSocketService**

```typescript
import { Injectable, inject, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { MessageOutput } from './chat.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private client: Client | null = null;
  private subscriptions = new Map<string, { unsubscribe: () => void; subject: Subject<MessageOutput> }>();

  async connect(): Promise<void> {
    if (this.client?.connected) return;

    const token = await this.authService.getIdToken();
    if (!token) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    this.client.activate();
  }

  subscribeToChat(chatId: string): Observable<MessageOutput> {
    const existing = this.subscriptions.get(chatId);
    if (existing) return existing.subject.asObservable();

    const subject = new Subject<MessageOutput>();

    const trySubscribe = () => {
      if (!this.client?.connected) {
        setTimeout(trySubscribe, 500);
        return;
      }

      const sub = this.client.subscribe(`/topic/chat/${chatId}`, (message: IMessage) => {
        const msg: MessageOutput = JSON.parse(message.body);
        subject.next(msg);
      });

      this.subscriptions.set(chatId, { unsubscribe: () => sub.unsubscribe(), subject });
    };

    trySubscribe();
    return subject.asObservable();
  }

  unsubscribeFromChat(chatId: string): void {
    const sub = this.subscriptions.get(chatId);
    if (sub) {
      sub.unsubscribe();
      sub.subject.complete();
      this.subscriptions.delete(chatId);
    }
  }

  disconnect(): void {
    this.subscriptions.forEach(sub => {
      sub.unsubscribe();
      sub.subject.complete();
    });
    this.subscriptions.clear();
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
```

- [ ] **Step 4: Update ChatPageComponent to use WebSocket instead of polling**

In `chat-page.component.ts`:

Add the import and inject:

```typescript
import { WebSocketService } from '../../core/services/websocket.service';
```

```typescript
private readonly wsService = inject(WebSocketService);
private wsSubscription: import('rxjs').Subscription | null = null;
```

Remove the `pollingInterval` field. Replace `startPolling()` and `stopPolling()`:

```typescript
private startWebSocket(chatId: string): void {
  this.stopWebSocket();
  this.wsSubscription = this.wsService.subscribeToChat(chatId).subscribe({
    next: (msg) => this.messages.update(msgs => [...msgs, msg])
  });
}

private stopWebSocket(): void {
  if (this.wsSubscription) {
    this.wsSubscription.unsubscribe();
    this.wsSubscription = null;
  }
  const chat = this.activeChat();
  if (chat) {
    this.wsService.unsubscribeFromChat(chat.id);
  }
}
```

In `ngOnInit()`, add WebSocket connection after user load:

```typescript
this.wsService.connect();
```

In `openChat()`, replace `this.startPolling(chat.id)` with `this.startWebSocket(chat.id)`.

In `ngOnDestroy()`, replace `this.stopPolling()` with:

```typescript
this.stopWebSocket();
this.wsService.disconnect();
```

In `sendMessage()`, remove the optimistic append (`this.messages.update(msgs => [...msgs, msg])`) — the message will arrive via the WebSocket subscription instead. Keep the `messageText.set('')` and `sendingMessage.set(false)` in the `next` callback.

- [ ] **Step 5: Verify frontend compiles**

Run: `cd frontend && npm run build`

Expected: Build succeeds

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/core/services/websocket.service.ts frontend/src/app/features/chat/chat-page.component.ts frontend/src/environments/ frontend/package.json frontend/package-lock.json
git commit -m "feat: replace chat polling with WebSocket/STOMP real-time messaging"
```

---

### Task 7: Android — Add dependencies

**Files:**
- Modify: `YTShare.Android/app/build.gradle.kts`

- [ ] **Step 1: Add Krossbow and FCM dependencies**

In the `dependencies` block of `app/build.gradle.kts`, add:

```kotlin
// Krossbow STOMP client
implementation("org.hildan.krossbow:krossbow-stomp-core:7.0.0")
implementation("org.hildan.krossbow:krossbow-websocket-okhttp:7.0.0")

// Firebase Cloud Messaging
implementation("com.google.firebase:firebase-messaging-ktx")
```

Note: `firebase-messaging-ktx` version is managed by the Firebase BOM already declared.

- [ ] **Step 2: Sync Gradle**

Run: `cd YTShare.Android && ./gradlew app:dependencies --configuration releaseRuntimeClasspath | head -20`

Expected: Krossbow and firebase-messaging appear in dependency tree.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/build.gradle.kts
git commit -m "feat: add Krossbow STOMP and FCM dependencies to Android app"
```

---

### Task 8: Android — Create DTOs

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/UserOutputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/FriendshipOutputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/FriendshipInputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/ChatOutputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/ChatInputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/MessageOutputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/MessageInputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/DeviceTokenInputDto.kt`

- [ ] **Step 1: Create all DTO files**

`UserOutputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserOutputDto(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
```

`FriendshipOutputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipOutputDto(
    val id: String,
    val user: UserOutputDto,
    val friend: UserOutputDto,
    val status: String
)
```

`FriendshipInputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipInputDto(
    val userId: String,
    val friendId: String,
    val status: String
)
```

`ChatOutputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatOutputDto(
    val id: String,
    val participants: List<UserOutputDto>
)
```

`ChatInputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatInputDto(
    val participantIds: List<String>
)
```

`MessageOutputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageOutputDto(
    val id: String,
    val content: String,
    val status: String,
    val chat: ChatOutputDto,
    val sender: UserOutputDto,
    val createdAt: String
)
```

`MessageInputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageInputDto(
    val content: String,
    val status: String,
    val chatId: String,
    val senderId: String
)
```

`DeviceTokenInputDto.kt`:
```kotlin
package com.example.ytshare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenInputDto(
    val fcmToken: String,
    val platform: String
)
```

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/dto/
git commit -m "feat: add chat, friendship, message, and device token DTOs"
```

---

### Task 9: Android — Create API services

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/UserApiService.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/FriendshipApiService.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/ChatApiService.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/MessageApiService.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/DeviceTokenApiService.kt`

- [ ] **Step 1: Create UserApiService**

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.UserOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UserApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/users"

    suspend fun getCurrentUser(): UserOutputDto {
        return client.get("$baseUrl/me").body()
    }

    suspend fun getUserByEmail(email: String): UserOutputDto {
        return client.get("$baseUrl/by-email") {
            parameter("email", email)
        }.body()
    }
}
```

- [ ] **Step 2: Create FriendshipApiService**

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.FriendshipInputDto
import com.example.ytshare.data.remote.dto.FriendshipOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FriendshipApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/friendships"

    suspend fun getFriendshipsByUser(userId: String): List<FriendshipOutputDto> {
        return client.get("$baseUrl/user/$userId").body()
    }

    suspend fun getFriendshipsByStatus(userId: String, status: String): List<FriendshipOutputDto> {
        return client.get("$baseUrl/user/$userId/status") {
            parameter("status", status)
        }.body()
    }

    suspend fun sendFriendRequest(input: FriendshipInputDto): FriendshipOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    suspend fun updateFriendshipStatus(id: String, status: String): FriendshipOutputDto {
        return client.patch("$baseUrl/$id/status") {
            parameter("status", status)
        }.body()
    }

    suspend fun deleteFriendship(id: String) {
        client.delete("$baseUrl/$id")
    }
}
```

- [ ] **Step 3: Create ChatApiService**

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.ChatInputDto
import com.example.ytshare.data.remote.dto.ChatOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ChatApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/chats"

    suspend fun getAllChats(): List<ChatOutputDto> {
        return client.get("$baseUrl/all").body()
    }

    suspend fun createChat(input: ChatInputDto): ChatOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
```

- [ ] **Step 4: Create MessageApiService**

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.MessageInputDto
import com.example.ytshare.data.remote.dto.MessageOutputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class MessageApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/messages"

    suspend fun getMessagesByChat(chatId: String): List<MessageOutputDto> {
        return client.get("$baseUrl/chat/$chatId").body()
    }

    suspend fun sendMessage(input: MessageInputDto): MessageOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }
}
```

- [ ] **Step 5: Create DeviceTokenApiService**

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import com.example.ytshare.data.remote.dto.DeviceTokenInputDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class DeviceTokenApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/device-tokens"

    suspend fun registerToken(input: DeviceTokenInputDto) {
        client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }
    }

    suspend fun removeToken() {
        client.delete(baseUrl)
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/UserApiService.kt YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/FriendshipApiService.kt YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/ChatApiService.kt YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/MessageApiService.kt YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/DeviceTokenApiService.kt
git commit -m "feat: add Ktor API services for chat, friendship, message, user, and device token"
```

---

### Task 10: Android — StompSessionManager

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/StompSessionManager.kt`

- [ ] **Step 1: Create StompSessionManager**

```kotlin
package com.example.ytshare.data.remote

import android.util.Log
import com.example.ytshare.Constants
import com.example.ytshare.data.auth.AuthRepository
import com.example.ytshare.data.remote.dto.MessageOutputDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class StompSessionManager(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var session: StompSession? = null
    private var connectJob: Job? = null
    private val subscriptionJobs = mutableMapOf<String, Job>()
    private val messageFlows = mutableMapOf<String, MutableSharedFlow<MessageOutputDto>>()

    private val wsUrl: String
        get() {
            val baseUrl = Constants.BACKEND_BASE_URL.replace("/api", "")
            return "$baseUrl/ws/websocket"
        }

    fun connect() {
        if (session != null) return
        connectJob = scope.launch {
            var delayMs = 1000L
            while (true) {
                try {
                    val token = authRepository.getIdToken() ?: break
                    val okHttpClient = OkHttpClient.Builder().build()
                    val client = StompClient(OkHttpWebSocketClient(okHttpClient))
                    session = client.connect(
                        url = wsUrl,
                        customStompConnectHeaders = mapOf("Authorization" to "Bearer $token")
                    )
                    Log.d("StompSessionManager", "Connected to STOMP")
                    delayMs = 1000L
                    break
                } catch (e: Exception) {
                    Log.w("StompSessionManager", "STOMP connect failed, retrying in ${delayMs}ms", e)
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(30_000L)
                }
            }
        }
    }

    fun subscribe(chatId: String): Flow<MessageOutputDto> {
        val existing = messageFlows[chatId]
        if (existing != null) return existing

        val flow = MutableSharedFlow<MessageOutputDto>(extraBufferCapacity = 64)
        messageFlows[chatId] = flow

        val job = scope.launch {
            connectJob?.join()
            val currentSession = session ?: return@launch
            try {
                val textMessages = currentSession.subscribeText("/topic/chat/$chatId")
                textMessages.collect { text ->
                    val msg = json.decodeFromString<MessageOutputDto>(text)
                    flow.emit(msg)
                }
            } catch (e: Exception) {
                Log.w("StompSessionManager", "Subscription to chat $chatId failed", e)
            }
        }
        subscriptionJobs[chatId] = job

        return flow
    }

    fun unsubscribe(chatId: String) {
        subscriptionJobs.remove(chatId)?.cancel()
        messageFlows.remove(chatId)
    }

    fun disconnect() {
        subscriptionJobs.values.forEach { it.cancel() }
        subscriptionJobs.clear()
        messageFlows.clear()
        connectJob?.cancel()
        scope.launch {
            try {
                session?.disconnect()
            } catch (_: Exception) {}
            session = null
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/StompSessionManager.kt
git commit -m "feat: add StompSessionManager for real-time WebSocket messaging"
```

---

### Task 11: Android — ChatRepository

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/ChatRepository.kt`

- [ ] **Step 1: Create ChatRepository**

```kotlin
package com.example.ytshare.data.repository

import com.example.ytshare.data.remote.ChatApiService
import com.example.ytshare.data.remote.DeviceTokenApiService
import com.example.ytshare.data.remote.FriendshipApiService
import com.example.ytshare.data.remote.MessageApiService
import com.example.ytshare.data.remote.UserApiService
import com.example.ytshare.data.remote.dto.ChatInputDto
import com.example.ytshare.data.remote.dto.ChatOutputDto
import com.example.ytshare.data.remote.dto.DeviceTokenInputDto
import com.example.ytshare.data.remote.dto.FriendshipInputDto
import com.example.ytshare.data.remote.dto.FriendshipOutputDto
import com.example.ytshare.data.remote.dto.MessageInputDto
import com.example.ytshare.data.remote.dto.MessageOutputDto
import com.example.ytshare.data.remote.dto.UserOutputDto

class ChatRepository(
    private val userApi: UserApiService,
    private val friendshipApi: FriendshipApiService,
    private val chatApi: ChatApiService,
    private val messageApi: MessageApiService,
    private val deviceTokenApi: DeviceTokenApiService
) {
    private var cachedCurrentUser: UserOutputDto? = null

    suspend fun getCurrentUser(): UserOutputDto {
        cachedCurrentUser?.let { return it }
        val user = userApi.getCurrentUser()
        cachedCurrentUser = user
        return user
    }

    fun clearCachedUser() {
        cachedCurrentUser = null
    }

    suspend fun getFriends(): List<FriendshipOutputDto> {
        val user = getCurrentUser()
        return friendshipApi.getFriendshipsByStatus(user.id, "ACCEPTED")
    }

    suspend fun getPendingRequests(): List<FriendshipOutputDto> {
        val user = getCurrentUser()
        return friendshipApi.getFriendshipsByStatus(user.id, "PENDING")
    }

    suspend fun sendFriendRequest(email: String) {
        val user = getCurrentUser()
        val foundUser = userApi.getUserByEmail(email)
        friendshipApi.sendFriendRequest(
            FriendshipInputDto(userId = user.id, friendId = foundUser.id, status = "PENDING")
        )
    }

    suspend fun acceptFriendRequest(friendshipId: String) {
        friendshipApi.updateFriendshipStatus(friendshipId, "ACCEPTED")
    }

    suspend fun rejectFriendRequest(friendshipId: String) {
        friendshipApi.updateFriendshipStatus(friendshipId, "REJECTED")
    }

    suspend fun removeFriend(friendshipId: String) {
        friendshipApi.deleteFriendship(friendshipId)
    }

    suspend fun getOrCreateChat(friendId: String): ChatOutputDto {
        val chats = chatApi.getAllChats()
        val existing = chats.find { chat ->
            chat.participants.any { it.id == friendId }
        }
        if (existing != null) return existing

        val user = getCurrentUser()
        return chatApi.createChat(ChatInputDto(participantIds = listOf(user.id, friendId)))
    }

    suspend fun getMessages(chatId: String): List<MessageOutputDto> {
        return messageApi.getMessagesByChat(chatId)
    }

    suspend fun sendMessage(chatId: String, content: String): MessageOutputDto {
        val user = getCurrentUser()
        return messageApi.sendMessage(
            MessageInputDto(content = content, status = "SENT", chatId = chatId, senderId = user.id)
        )
    }

    suspend fun registerDeviceToken(fcmToken: String) {
        deviceTokenApi.registerToken(DeviceTokenInputDto(fcmToken = fcmToken, platform = "ANDROID"))
    }

    suspend fun removeDeviceToken() {
        deviceTokenApi.removeToken()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/ChatRepository.kt
git commit -m "feat: add ChatRepository coordinating friendship, chat, and message APIs"
```

---

### Task 12: Android — FriendsViewModel and FriendsScreen

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsViewModel.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsScreen.kt`

- [ ] **Step 1: Create FriendsViewModel**

```kotlin
package com.example.ytshare.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.data.remote.dto.FriendshipOutputDto
import com.example.ytshare.data.remote.dto.UserOutputDto
import com.example.ytshare.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _friends = MutableStateFlow<List<FriendshipOutputDto>>(emptyList())
    val friends: StateFlow<List<FriendshipOutputDto>> = _friends

    private val _pendingRequests = MutableStateFlow<List<FriendshipOutputDto>>(emptyList())
    val pendingRequests: StateFlow<List<FriendshipOutputDto>> = _pendingRequests

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<UserOutputDto?>(null)
    val currentUser: StateFlow<UserOutputDto?> = _currentUser

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentUser.value = repository.getCurrentUser()
                _friends.value = repository.getFriends()
                _pendingRequests.value = repository.getPendingRequests()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to load data", e)
                _snackbarMessage.value = "Couldn't load friends. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(email: String) {
        viewModelScope.launch {
            try {
                val currentEmail = _currentUser.value?.email
                if (email.equals(currentEmail, ignoreCase = true)) {
                    _snackbarMessage.value = "You can't add yourself as a friend"
                    return@launch
                }
                repository.sendFriendRequest(email)
                _snackbarMessage.value = "Friend request sent!"
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to send request", e)
                _snackbarMessage.value = "No user found with that email"
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to accept", e)
                _snackbarMessage.value = "Failed to accept request"
            }
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.rejectFriendRequest(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to reject", e)
                _snackbarMessage.value = "Failed to reject request"
            }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            try {
                repository.removeFriend(friendshipId)
                loadData()
            } catch (e: Exception) {
                Log.e("FriendsViewModel", "Failed to remove friend", e)
                _snackbarMessage.value = "Failed to remove friend"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun isIncomingRequest(friendship: FriendshipOutputDto): Boolean {
        return _currentUser.value?.let { friendship.friend.id == it.id } ?: false
    }

    fun getFriendUser(friendship: FriendshipOutputDto): UserOutputDto {
        val current = _currentUser.value
        return if (current != null && friendship.user.id == current.id) friendship.friend else friendship.user
    }
}
```

- [ ] **Step 2: Create FriendsScreen**

```kotlin
package com.example.ytshare.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytshare.data.remote.dto.FriendshipOutputDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onFriendClick: (friendUserId: String) -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<FriendshipOutputDto?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Red,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Friend")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isLoading && friends.isEmpty() && pendingRequests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No friends yet. Tap + to add someone.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            SectionHeader("PENDING REQUESTS (${pendingRequests.size})")
                        }
                        items(pendingRequests, key = { it.id }) { request ->
                            PendingRequestRow(
                                friendship = request,
                                isIncoming = viewModel.isIncomingRequest(request),
                                friendUser = viewModel.getFriendUser(request),
                                onAccept = { viewModel.acceptRequest(request.id) },
                                onReject = { viewModel.rejectRequest(request.id) }
                            )
                        }
                    }
                    if (friends.isNotEmpty()) {
                        item {
                            SectionHeader("FRIENDS (${friends.size})")
                        }
                        items(friends, key = { it.id }) { friendship ->
                            val friendUser = viewModel.getFriendUser(friendship)
                            FriendRow(
                                name = "${friendUser.firstName} ${friendUser.lastName}",
                                email = friendUser.email,
                                onClick = { onFriendClick(friendUser.id) },
                                onLongClick = { showRemoveDialog = friendship }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onSend = { email ->
                viewModel.sendFriendRequest(email)
                showAddDialog = false
            }
        )
    }

    showRemoveDialog?.let { friendship ->
        val friendUser = viewModel.getFriendUser(friendship)
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove Friend") },
            text = { Text("Remove ${friendUser.firstName} ${friendUser.lastName} from your friends?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFriend(friendship.id)
                    showRemoveDialog = null
                }) { Text("Remove", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFEF9A9A),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun Avatar(letter: Char, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size / 2.5).sp
        )
    }
}

private val avatarColors = listOf(
    Color(0xFF6A1B9A), Color(0xFF00695C), Color(0xFFE65100),
    Color(0xFF1565C0), Color(0xFFC62828), Color(0xFF2E7D32)
)

private fun avatarColor(name: String): Color {
    return avatarColors[name.hashCode().mod(avatarColors.size).let { if (it < 0) it + avatarColors.size else it }]
}

@Composable
private fun FriendRow(name: String, email: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(letter = name.first(), color = avatarColor(name))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(email, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun PendingRequestRow(
    friendship: FriendshipOutputDto,
    isIncoming: Boolean,
    friendUser: com.example.ytshare.data.remote.dto.UserOutputDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(letter = friendUser.email.first(), color = avatarColor(friendUser.email))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friendUser.email, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (isIncoming) "Incoming request" else "Outgoing · Pending",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        if (isIncoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Accept", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Reject", fontSize = 12.sp) }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column {
                Text("Enter the email address of the person you want to add.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("friend@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onSend(email.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Send Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsViewModel.kt YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/FriendsScreen.kt
git commit -m "feat: add FriendsScreen with friend list, pending requests, and add friend dialog"
```

---

### Task 13: Android — ConversationViewModel and ConversationScreen

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationViewModel.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationScreen.kt`

- [ ] **Step 1: Create ConversationViewModel**

```kotlin
package com.example.ytshare.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.data.remote.StompSessionManager
import com.example.ytshare.data.remote.dto.MessageOutputDto
import com.example.ytshare.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val repository: ChatRepository,
    private val stompSessionManager: StompSessionManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageOutputDto>>(emptyList())
    val messages: StateFlow<List<MessageOutputDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _friendName = MutableStateFlow("")
    val friendName: StateFlow<String> = _friendName

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private var chatId: String? = null
    private var currentUserId: String? = null
    private var subscriptionJob: Job? = null

    fun load(friendId: String, friendFirstName: String, friendLastName: String) {
        _friendName.value = "$friendFirstName $friendLastName"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.getCurrentUser()
                currentUserId = user.id
                val chat = repository.getOrCreateChat(friendId)
                chatId = chat.id
                _messages.value = repository.getMessages(chat.id)
                subscribe()
            } catch (e: Exception) {
                Log.e("ConversationVM", "Failed to load messages", e)
                _snackbarMessage.value = "Couldn't load messages. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun subscribe() {
        val id = chatId ?: return
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            stompSessionManager.subscribe(id).collect { msg ->
                val current = _messages.value
                if (current.none { it.id == msg.id }) {
                    _messages.value = current + msg
                }
            }
        }
    }

    fun unsubscribe() {
        subscriptionJob?.cancel()
        chatId?.let { stompSessionManager.unsubscribe(it) }
    }

    fun sendMessage(content: String) {
        val id = chatId ?: return
        viewModelScope.launch {
            try {
                repository.sendMessage(id, content)
            } catch (e: Exception) {
                Log.e("ConversationVM", "Failed to send message", e)
                _snackbarMessage.value = "Message failed to send."
            }
        }
    }

    fun isOwnMessage(msg: MessageOutputDto): Boolean {
        return msg.sender.id == currentUserId
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribe()
    }
}
```

- [ ] **Step 2: Create ConversationScreen**

```kotlin
package com.example.ytshare.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytshare.data.remote.dto.MessageOutputDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    friendId: String,
    friendFirstName: String,
    friendLastName: String,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val friendName by viewModel.friendName.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.load(friendId, friendFirstName, friendLastName)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.unsubscribe() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6A1B9A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                friendName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(friendName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Say hello!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            isOwn = viewModel.isOwnMessage(msg)
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            viewModel.sendMessage(text)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Color.Red else Color.Gray)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageOutputDto, isOwn: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    )
                )
                .background(if (isOwn) Color.Red else Color(0xFF333333))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        Text(
            text = formatTimestamp(message.createdAt),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun formatTimestamp(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: return iso
        SimpleDateFormat("h:mm a", Locale.US).format(date)
    } catch (_: Exception) {
        iso
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationViewModel.kt YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/chat/ConversationScreen.kt
git commit -m "feat: add ConversationScreen with real-time STOMP messaging"
```

---

### Task 14: Android — FCM service

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/fcm/YTShareFirebaseMessagingService.kt`
- Modify: `YTShare.Android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create YTShareFirebaseMessagingService**

```kotlin
package com.example.ytshare.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ytshare.MainActivityCompose
import com.example.ytshare.R
import com.example.ytshare.data.repository.ChatRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class YTShareFirebaseMessagingService : FirebaseMessagingService() {

    private val chatRepository: ChatRepository by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            try {
                chatRepository.registerDeviceToken(token)
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["senderName"] ?: "New Message"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val senderId = message.data["senderId"]

        createNotificationChannel()

        val intent = Intent(this, MainActivityCompose::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            senderId?.let { putExtra("openChatWithFriendId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "chat_messages"
    }
}
```

- [ ] **Step 2: Register service in AndroidManifest.xml**

Add inside the `<application>` tag, before the closing `</application>`:

```xml
<service
    android:name=".fcm.YTShareFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/fcm/ YTShare.Android/app/src/main/AndroidManifest.xml
git commit -m "feat: add FCM service for push notification handling"
```

---

### Task 15: Android — Koin modules, navigation, and wiring

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt`
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt`

- [ ] **Step 1: Update AppModules.kt**

Add new imports at top:

```kotlin
import com.example.ytshare.data.remote.ChatApiService
import com.example.ytshare.data.remote.DeviceTokenApiService
import com.example.ytshare.data.remote.FriendshipApiService
import com.example.ytshare.data.remote.MessageApiService
import com.example.ytshare.data.remote.StompSessionManager
import com.example.ytshare.data.remote.UserApiService
import com.example.ytshare.data.repository.ChatRepository
import com.example.ytshare.ui.screens.chat.ConversationViewModel
import com.example.ytshare.ui.screens.chat.FriendsViewModel
```

Add to `networkModule` after the `VideoApiService` single:

```kotlin
single { UserApiService(get()) }
single { FriendshipApiService(get()) }
single { ChatApiService(get()) }
single { MessageApiService(get()) }
single { DeviceTokenApiService(get()) }
single { StompSessionManager(get()) }
```

Add to `repositoryModule` after the `VideoRepository` single:

```kotlin
single { ChatRepository(get(), get(), get(), get(), get()) }
```

Add to `viewModelModule` after the `AuthViewModel` viewModel:

```kotlin
viewModel { FriendsViewModel(get()) }
viewModel { ConversationViewModel(get(), get()) }
```

- [ ] **Step 2: Update MainActivityCompose.kt — add Chat nav tab and routes**

Add new imports:

```kotlin
import androidx.compose.material.icons.filled.Chat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ytshare.ui.screens.chat.ConversationScreen
import com.example.ytshare.ui.screens.chat.ConversationViewModel
import com.example.ytshare.ui.screens.chat.FriendsScreen
import com.example.ytshare.ui.screens.chat.FriendsViewModel
```

Update the `bottomNavItems` list to add Chat between History and Settings:

```kotlin
val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Filled.Home, "Home"),
    BottomNavItem("history", Icons.Filled.History, "History"),
    BottomNavItem("friends", Icons.Filled.Chat, "Chat"),
    BottomNavItem("settings", Icons.Filled.Settings, "Settings")
)
```

Inside the `NavHost` block in `MainScreen()`, add the friends and conversation routes after the settings composable:

```kotlin
composable("friends") {
    val friendsViewModel: FriendsViewModel = koinViewModel()
    FriendsScreen(
        viewModel = friendsViewModel,
        onFriendClick = { friendUserId ->
            navController.navigate("conversation/$friendUserId")
        }
    )
}

composable(
    route = "conversation/{friendId}/{firstName}/{lastName}",
    arguments = listOf(
        navArgument("friendId") { type = NavType.StringType },
        navArgument("firstName") { type = NavType.StringType },
        navArgument("lastName") { type = NavType.StringType }
    )
) { backStackEntry ->
    val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
    val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
    val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
    val conversationViewModel: ConversationViewModel = koinViewModel()
    ConversationScreen(
        viewModel = conversationViewModel,
        friendId = friendId,
        friendFirstName = firstName,
        friendLastName = lastName,
        onBack = { navController.popBackStack() }
    )
}
```

Update the `onFriendClick` in the friends composable to pass friend name data. This requires the `FriendsScreen` to pass the full friend info. Update the `onFriendClick` lambda:

```kotlin
composable("friends") {
    val friendsViewModel: FriendsViewModel = koinViewModel()
    FriendsScreen(
        viewModel = friendsViewModel,
        onFriendClick = { friendUserId ->
            val friendship = friendsViewModel.friends.value.find { f ->
                friendsViewModel.getFriendUser(f).id == friendUserId
            }
            val friendUser = friendship?.let { friendsViewModel.getFriendUser(it) }
            val firstName = friendUser?.firstName ?: ""
            val lastName = friendUser?.lastName ?: ""
            navController.navigate("conversation/$friendUserId/$firstName/$lastName")
        }
    )
}
```

Also hide the bottom nav bar when on the conversation screen. Update the `Scaffold` to conditionally show the bottom bar:

```kotlin
val currentRoute = navBackStackEntry?.destination?.route

Scaffold(
    bottomBar = {
        if (currentRoute?.startsWith("conversation") != true) {
            NavigationBar(
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                // ... existing nav bar items
            }
        }
    }
)
```

- [ ] **Step 3: Connect STOMP on login**

In the `MainScreen()` composable, add a `LaunchedEffect` to connect STOMP and register FCM token when the screen first appears. Add at the top of `MainScreen()`:

```kotlin
val stompManager: StompSessionManager = org.koin.compose.koinInject()
val chatRepo: ChatRepository = org.koin.compose.koinInject()

LaunchedEffect(Unit) {
    stompManager.connect()
    try {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { chatRepo.registerDeviceToken(token) } catch (_: Exception) {}
            }
        }
    } catch (_: Exception) {}
}
```

- [ ] **Step 4: Verify the Android project compiles**

Run: `cd YTShare.Android && ./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt
git commit -m "feat: wire up chat navigation, Koin modules, and STOMP connection on login"
```

---

## Post-Implementation Checklist

After all tasks are complete:

- [ ] Backend compiles: `cd backend && ./mvnw compile -q`
- [ ] Frontend compiles: `cd frontend && npm run build`
- [ ] Android compiles: `cd YTShare.Android && ./gradlew assembleDebug`
- [ ] Start backend, verify WebSocket endpoint accessible at `ws://localhost:8080/ws`
- [ ] Start frontend, verify chat uses WebSocket (no more polling in Network tab)
- [ ] Install Android APK on device, verify Chat tab appears and friends/conversation screens work
- [ ] Test FCM: send a message from frontend while Android app is backgrounded, verify push notification appears
