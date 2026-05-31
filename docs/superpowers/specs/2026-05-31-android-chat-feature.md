# Android Chat Feature — Design Spec

## Overview

Add a chat feature to the Android app, consisting of a Friends screen (friend list, add/accept/reject requests) and a Conversation screen (messaging with a friend). Uses the existing backend Chat, Message, and Friendship REST APIs. Network-only — no local Room caching for chat data. Includes Firebase Cloud Messaging (FCM) push notifications for background message delivery.

## Goals

- Add a "Chat" tab to the bottom navigation bar
- Friends screen as the hub: friend list, pending requests, add friend by email
- Conversation screen: message bubbles, 3-second polling, send messages
- FCM push notifications for new messages when the app is backgrounded
- Reuse existing Ktor HttpClient with Firebase auth token injection
- Follow existing app patterns (Koin DI, Compose + MD3, MVVM)

## Non-Goals

- Local Room caching for chat/friendship data (network-only)
- Offline message queue or sync
- WebSocket/STOMP real-time transport (backend doesn't support it)
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
- `getMessages(chatId: String): List<MessageOutputDto>` — GET messages for chat
- `sendMessage(chatId: String, content: String)` — POST message with status SENT
- `getCurrentUser(): UserOutputDto` — GET `/users/me`, cached in memory for the session

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

- `networkModule` — add `FriendshipApiService`, `ChatApiService`, `MessageApiService`, `UserApiService`, `DeviceTokenApiService`
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
- **Polling**: fetch messages every 3 seconds while the screen is visible. Stop polling on navigate away or lifecycle pause.
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
- Actions: `loadMessages(friendId)`, `sendMessage(content)`, `startPolling()`, `stopPolling()`
- Internally manages the `chatId` — resolves it on first load via `ChatRepository.getOrCreateChat()`

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

All other dependencies (Ktor, kotlinx.serialization, Koin, Compose, Firebase Auth) are already present.

### Backend

| Library | Purpose |
|---------|---------|
| Firebase Admin SDK | Already present — used for sending FCM push notifications |

---

## Future Work (Out of Scope)

- Group chats (multi-participant conversations)
- Message read receipts (DeliveryStatus.READ)
- WebSocket/STOMP for real-time messaging
- Typing indicators
- Media messages (images, videos)
- Message search
- Local Room cache for offline chat access
