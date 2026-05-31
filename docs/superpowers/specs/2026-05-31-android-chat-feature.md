# Android Chat Feature — Design Spec

## Overview

Add a chat feature to the Android app, consisting of a Friends screen (friend list, add/accept/reject requests) and a Conversation screen (messaging with a friend). Uses the existing backend Chat, Message, and Friendship REST APIs. Network-only — no local Room caching for chat data. Real-time messaging via WebSocket/STOMP. Includes Firebase Cloud Messaging (FCM) push notifications for background message delivery.

## Goals

- Add a "Chat" tab to the bottom navigation bar
- Friends screen as the hub: friend list, pending requests, add friend by email
- Conversation screen: message bubbles, real-time delivery via WebSocket/STOMP, send messages
- FCM push notifications for new messages when the app is backgrounded
- Reuse existing Ktor HttpClient with Firebase auth token injection
- Follow existing app patterns (Koin DI, Compose + MD3, MVVM)

## Non-Goals

- Local Room caching for chat/friendship data (network-only)
- Offline message queue or sync
- WebSocket fallback to polling (if STOMP connection fails, chat is unavailable until reconnect)
- Group chats (backend supports multiple participants, but UI is 1:1 only)
- Message read receipts (DeliveryStatus.READ not used)
- Message editing or reactions

---

## Architecture

### Backend Changes

**New: FCM Device Token Registration**

The backend needs a way to store FCM device tokens and send push notifications.

**DeviceToken entity:**

| Field | Type | Notes |
|-------|------|-------|
| `id` | `UUID` | Primary key |
| `user` | `User` | ManyToOne, the token owner |
| `fcmToken` | `String` | FCM registration token, unique |
| `platform` | `String` | "ANDROID" (future-proofs for iOS) |
| `createdAt` | `Date` | Audit field |
| `updatedAt` | `Date` | Audit field |

**DeviceToken endpoints:**

- `POST /device-tokens` — register or update FCM token (accepts `DeviceTokenInputDto` with `fcmToken` and `platform`)
- `DELETE /device-tokens` — remove token on logout

**DeviceTokenInputDto:**
- `String fcmToken`
- `String platform`

**Push notification on message creation:**

In `MessageService.create()`, after saving the message, look up all chat participants except the sender, find their registered FCM tokens, and send a push notification via Firebase Admin SDK with:
- `title`: sender's first name
- `body`: message content (truncated to 100 chars)
- `data`: `chatId`, `senderId`, `senderName` (for deep link navigation)

**Liquibase:** New changelog to create `device_tokens` table with columns `id`, `user_id` (FK to users), `fcm_token` (unique), `platform`, `created_at`, `updated_at`.

### WebSocket/STOMP (Backend)

**Dependencies to add:**

- `spring-boot-starter-websocket`

**WebSocketConfig:**

- `@EnableWebSocketMessageBroker`
- Simple in-memory message broker with destination prefix `/topic`
- Application destination prefix `/app`
- STOMP endpoint: `/ws` with SockJS fallback and allowed origins matching CORS config (add Android's origin)
- Registry: `registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS()`

**Authentication:**

- Custom `ChannelInterceptor` on the `clientInboundChannel` that intercepts `CONNECT` frames
- Reads the `Authorization` header (Bearer token) from the STOMP `CONNECT` frame's native headers
- Validates the Firebase token via the same `FirebaseAuth.getInstance().verifyIdToken()` used by `FirebaseTokenFilter`
- Sets a `UsernamePasswordAuthenticationToken` (or `FirebaseAuthenticationToken`) on the STOMP session's `simpUser` principal
- Rejects connection if token is missing or invalid

**Message broadcasting:**

- In `MessageService.create()`, after saving the message to the database, use `SimpMessagingTemplate.convertAndSend()` to broadcast the `MessageOutputDto` to `/topic/chat/{chatId}`
- All clients subscribed to that chat topic receive the message in real-time
- This replaces the need for clients to poll `GET /messages/chat/{chatId}`

**STOMP destinations:**

| Destination | Direction | Purpose |
|-------------|-----------|---------|
| `/topic/chat/{chatId}` | Server → Client | New messages broadcast to chat participants |
| `/app/chat.send` | Client → Server | Client sends a message (alternative to REST POST, optional — REST `POST /messages` is kept as the primary send mechanism) |

**Note:** Messages are still sent via REST `POST /messages`. The STOMP topic is for receiving only. This keeps the send path simple (Ktor HTTP on Android) and avoids duplicating validation logic in a STOMP controller.

### Frontend Update

Update the frontend chat page to use WebSocket/STOMP instead of polling:

- Add `@stomp/stompjs` and `sockjs-client` dependencies
- Create a `WebSocketService` that connects to `/ws` with the Firebase Bearer token
- Subscribe to `/topic/chat/{chatId}` when a chat is open
- On message received, append to the messages list
- Remove the 3-second `setInterval` polling
- Reconnect on disconnect with exponential backoff
- Fall back to REST fetch on initial load (get message history), then switch to WebSocket for live updates

### Data Layer (Android)

No new Room entities. All chat data is fetched from the network on demand.

### Networking Layer (Ktor Client)

Reuse the existing `HttpClient` from `AppModules.kt` (already configured with CIO engine, kotlinx.serialization, and Firebase Bearer token injection).

**New DTOs:**

```kotlin
@Serializable
data class UserOutputDto(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class FriendshipOutputDto(
    val id: String,
    val user: UserOutputDto,
    val friend: UserOutputDto,
    val status: String // "PENDING", "ACCEPTED", "REJECTED"
)

@Serializable
data class FriendshipInputDto(
    val userId: String,
    val friendId: String,
    val status: String
)

@Serializable
data class ChatOutputDto(
    val id: String,
    val participants: List<UserOutputDto>
)

@Serializable
data class ChatInputDto(
    val participantIds: List<String>
)

@Serializable
data class MessageOutputDto(
    val id: String,
    val content: String,
    val status: String,
    val chat: ChatOutputDto,
    val sender: UserOutputDto,
    val createdAt: String // ISO 8601 timestamp, used for message bubble times
)

@Serializable
data class MessageInputDto(
    val content: String,
    val status: String,
    val chatId: String,
    val senderId: String
)

@Serializable
data class DeviceTokenInputDto(
    val fcmToken: String,
    val platform: String
)
```

**FriendshipApiService:**

- `getFriendships(userId: String): List<FriendshipOutputDto>` — GET `/friendships/user/{userId}`
- `getFriendshipsByStatus(userId: String, status: String): List<FriendshipOutputDto>` — GET `/friendships/user/{userId}/status?status={status}`
- `sendFriendRequest(input: FriendshipInputDto): FriendshipOutputDto` — POST `/friendships`
- `updateFriendshipStatus(id: String, status: String): FriendshipOutputDto` — PATCH `/friendships/{id}/status?status={status}`
- `deleteFriendship(id: String)` — DELETE `/friendships/{id}`

**ChatApiService:**

- `getAllChats(): List<ChatOutputDto>` — GET `/chats/all`
- `createChat(input: ChatInputDto): ChatOutputDto` — POST `/chats`

**MessageApiService:**

- `getMessagesByChat(chatId: String): List<MessageOutputDto>` — GET `/messages/chat/{chatId}`
- `sendMessage(input: MessageInputDto): MessageOutputDto` — POST `/messages`

**UserApiService:**

- `getUserByEmail(email: String): UserOutputDto` — GET `/users/email/{email}` (new backend endpoint needed)
- `getCurrentUser(): UserOutputDto` — GET `/users/me` (new backend endpoint needed)

**DeviceTokenApiService:**

- `registerToken(input: DeviceTokenInputDto)` — POST `/device-tokens`
- `removeToken()` — DELETE `/device-tokens`

### New Backend Endpoints Required

The existing backend needs two new user lookup endpoints:

- `GET /users/me` — returns the authenticated user's `UserOutputDto` (uses `SecurityUtils.requireCurrentUserUid()` to find user by Firebase UID)
- `GET /users/email/{email}` — returns `UserOutputDto` for the given email (used by "Add Friend" to look up users)

### Repository Layer

**ChatRepository** (not Room — a coordinating class):

- `getFriends(): List<FriendshipOutputDto>` — fetches accepted friendships for the current user
- `getPendingRequests(): List<FriendshipOutputDto>` — fetches pending friendships (both incoming and outgoing)
- `sendFriendRequest(email: String)` — looks up user by email, then POST friendship with status PENDING
- `acceptFriendRequest(friendshipId: String)` — PATCH status to ACCEPTED
- `rejectFriendRequest(friendshipId: String)` — PATCH status to REJECTED
- `removeFriend(friendshipId: String)` — DELETE friendship
- `getOrCreateChat(friendId: String): ChatOutputDto` — checks existing chats for a 1:1 with this friend, creates one if not found
- `getMessages(chatId: String): List<MessageOutputDto>` — GET messages for chat (initial load / history)
- `sendMessage(chatId: String, content: String)` — POST message with status SENT
- `getCurrentUser(): UserOutputDto` — GET `/users/me`, cached in memory for the session

### WebSocket/STOMP (Android)

**Library:** Krossbow (`com.joffrey.krossbow:krossbow-stomp-core` + `krossbow-websocket-okhttp`) — Kotlin-first STOMP client, coroutine-based, KMP-compatible.

**StompSessionManager:**

- Connects to `ws://{BACKEND_BASE_URL}/ws/websocket` (SockJS raw WebSocket path) with Firebase Bearer token in the CONNECT frame headers
- Manages a single STOMP session for the app's lifecycle
- Exposes `subscribe(chatId: String): Flow<MessageOutputDto>` — subscribes to `/topic/chat/{chatId}` and deserializes incoming frames
- Handles reconnection with exponential backoff (1s, 2s, 4s, max 30s) on disconnect
- Disconnects on logout

**Connection lifecycle:**
- Connect after successful login (when user is authenticated)
- Reconnect automatically on network change (use `ConnectivityManager` callback)
- Disconnect on logout (before clearing Firebase auth)

**Integration with ConversationViewModel:**
- On screen open: fetch message history via REST `GET /messages/chat/{chatId}`, then subscribe to `/topic/chat/{chatId}` for live updates
- Incoming STOMP messages are appended to the messages StateFlow
- On screen close: unsubscribe from the topic (but keep the STOMP connection alive for other chats / FCM fallback)

### FCM Integration (Android)

**FirebaseMessagingService subclass:**

- `onNewToken(token: String)` — registers the new FCM token with the backend via `POST /device-tokens`
- `onMessageReceived(message: RemoteMessage)` — shows a notification with sender name and message preview

**Token lifecycle:**
- On login → register FCM token with backend
- On token refresh → re-register with backend
- On logout → call `DELETE /device-tokens` to remove token, then `FirebaseMessaging.getInstance().deleteToken()`

**Notification behavior:**
- Only show notification when the app is in the background or the user is not on the conversation screen with that sender
- Tapping the notification deep-links to the conversation screen with the sender

### Dependency Injection (Koin)

Add to existing modules in `AppModules.kt`:

- `networkModule` — add `FriendshipApiService`, `ChatApiService`, `MessageApiService`, `UserApiService`, `DeviceTokenApiService`, `StompSessionManager`
- `repositoryModule` — add `ChatRepository`
- `viewModelModule` — add `FriendsViewModel`, `ConversationViewModel`

---

## UI Design

### Navigation

Add a "Chat" destination to the bottom navigation bar in `MainActivityCompose.kt`:

- Current: Home, History, Settings
- New: Home, History, **Chat**, Settings

The Chat tab navigates to a nested nav graph:
- `"friends"` — Friends screen (default)
- `"conversation/{friendId}"` — Conversation screen (receives the friend's **user UUID** as argument, not the friendship ID)

### Friends Screen

```
┌─────────────────────────────┐
│  TopAppBar: "Chat"  [+ Add] │
├─────────────────────────────┤
│  PENDING REQUESTS (2)       │
│  ┌─────────────────────────┐│
│  │ [J] john@example.com    ││
│  │     Incoming  [✓] [✕]   ││
│  └─────────────────────────┘│
│  ┌─────────────────────────┐│
│  │ [S] sarah@test.com      ││
│  │     Outgoing · Pending   ││
│  └─────────────────────────┘│
├─────────────────────────────┤
│  FRIENDS (3)                │
│  ┌─────────────────────────┐│
│  │ [A] Alice Johnson     › ││
│  │     alice@example.com    ││
│  └─────────────────────────┘│
│  ┌─────────────────────────┐│
│  │ [B] Bob Smith          › ││
│  │     bob@example.com      ││
│  └─────────────────────────┘│
│         ...                 │
├─────────────────────────────┤
│ Home  History  Chat  Settings│
└─────────────────────────────┘
```

**Components:**

- **TopAppBar** (MD3): title "Chat", "+ Add Friend" action that opens a dialog
- **Pending Requests section**: shown only when requests exist. Each row shows avatar (first letter), email, and whether incoming (Accept/Reject buttons) or outgoing ("Pending" label). Incoming = current user is the `friend` field. Outgoing = current user is the `user` field.
- **Friends section**: each row shows avatar, full name, email. Tapping navigates to conversation screen. Swipe-to-delete to remove friend (confirmation dialog).
- **Add Friend dialog**: email input field, Cancel/Send Request buttons
- **Empty state**: centered icon + "No friends yet. Tap + to add someone."
- **Loading state**: centered CircularProgressIndicator
- **Pull-to-refresh**: re-fetches friends and requests from backend
- **Error Snackbars**: "No user found with that email", "Already friends", "Request already sent", "Can't add yourself", network errors

### Conversation Screen

```
┌─────────────────────────────┐
│  ← [A] Alice Johnson        │
├─────────────────────────────┤
│         Today                │
│                              │
│  [A] Hey! Did you see that   │
│      new video?              │
│      10:23 AM                │
│                              │
│      Yeah! I just shared it  │
│      to the TV 😄       [me] │
│      10:24 AM                │
│                              │
│  [A] Nice! I'll check it     │
│      out later tonight       │
│      10:25 AM                │
│                              │
├─────────────────────────────┤
│  [Type a message...    ] [➤] │
└─────────────────────────────┘
```

**Components:**

- **TopAppBar** (MD3): back arrow, friend's avatar, friend's full name. Red background.
- **Messages list**: LazyColumn, scrolls to bottom on load and on new messages. Date separators between days.
- **Message bubbles**: sent messages (red, right-aligned, rounded corners), received messages (dark gray, left-aligned with small avatar). Timestamp below each bubble.
- **Input bar**: OutlinedTextField with rounded shape, send button (red circle with arrow icon). Send button disabled when input is empty.
- **Lazy chat creation**: if no chat exists with this friend, create one on first message send.
- **Real-time updates**: on screen open, fetch message history via REST, then subscribe to STOMP topic `/topic/chat/{chatId}` for live incoming messages. Unsubscribe on navigate away.
- **Empty state**: "No messages yet. Say hello!"

### FriendsViewModel

- `friends: StateFlow<List<FriendshipOutputDto>>` — accepted friendships
- `pendingRequests: StateFlow<List<FriendshipOutputDto>>` — pending friendships (incoming + outgoing)
- `isLoading: StateFlow<Boolean>`
- `currentUser: StateFlow<UserOutputDto?>` — cached current user info
- Actions: `loadFriends()`, `sendFriendRequest(email)`, `acceptRequest(id)`, `rejectRequest(id)`, `removeFriend(id)`, `refresh()`

### ConversationViewModel

- `messages: StateFlow<List<MessageOutputDto>>` — messages for the active chat
- `isLoading: StateFlow<Boolean>`
- `friendName: StateFlow<String>` — display name for the top bar
- Actions: `loadMessages(friendId)`, `sendMessage(content)`, `subscribe()`, `unsubscribe()`
- Internally manages the `chatId` — resolves it on first load via `ChatRepository.getOrCreateChat()`
- On `subscribe()`: subscribes to STOMP topic for the chat, appends incoming messages to StateFlow
- On `unsubscribe()`: unsubscribes from the topic (called on screen dispose)

---

## Empty & Error States

| Scenario | Behavior |
|----------|----------|
| No friends, no requests | "No friends yet. Tap + to add someone." |
| No messages in conversation | "No messages yet. Say hello!" |
| Network error loading friends | Snackbar: "Couldn't load friends. Check your connection." |
| Network error loading messages | Snackbar: "Couldn't load messages. Check your connection." |
| Network error sending message | Snackbar: "Message failed to send." Message not added to UI. |
| 401 from any endpoint | Redirect to login screen (existing Ktor auth behavior) |

---

## FCM Push Notifications

### Backend

- New `DeviceToken` entity and `device_tokens` table
- `POST /device-tokens` — upsert token (if token already exists for user, update; if user already has a token for this platform, replace)
- `DELETE /device-tokens` — delete all tokens for the authenticated user
- In `MessageService.create()`: after saving message, find FCM tokens for all chat participants except sender, send push via Firebase Admin SDK

### Android

- `YTShareFirebaseMessagingService extends FirebaseMessagingService`
- `onNewToken()` → POST token to backend
- `onMessageReceived()` → build notification with sender name as title, message content as body
- Notification channel: "Chat Messages" with default importance
- Tapping notification navigates to conversation screen with the sender (via deep link intent with `friendId` extra)
- Suppress notification if the app is in the foreground AND the user is on the conversation screen with that sender

---

## Dependencies to Add

### Android

| Library | Purpose |
|---------|---------|
| Firebase Cloud Messaging | Push notifications |
| Krossbow STOMP (krossbow-stomp-core, krossbow-websocket-okhttp) | STOMP client over WebSocket |

All other dependencies (Ktor, kotlinx.serialization, Koin, Compose, Firebase Auth) are already present.

### Backend

| Library | Purpose |
|---------|---------|
| spring-boot-starter-websocket | WebSocket/STOMP message broker |
| Firebase Admin SDK | Already present — used for sending FCM push notifications |

### Frontend

| Library | Purpose |
|---------|---------|
| @stomp/stompjs | STOMP client for browser |
| sockjs-client | SockJS fallback transport |

---

## Future Work (Out of Scope)

- Group chats (multi-participant conversations)
- Message read receipts (DeliveryStatus.READ)
- Typing indicators
- Media messages (images, videos)
- Message search
- Local Room cache for offline chat access
