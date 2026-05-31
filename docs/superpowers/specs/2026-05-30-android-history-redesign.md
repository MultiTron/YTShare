# Android History Redesign — Design Spec

## Overview

Redesign the Android app's history feature to use the remote Java backend as the primary data source, with Room as a local cache. Add Firebase Auth to the Android app. Rebuild the history screen in Jetpack Compose with Material Design 3. Remove the `description` field from the backend Video entity.

## Goals

- Connect Android app to the remote backend for video history (CRUD)
- Add Firebase Auth to the Android app (same Firebase project as frontend)
- Replace raw SQLite (`DBHelper`) with Room as a local cache
- Redesign the Compose history screen with MD3, search, sort, individual + bulk delete
- Use KMP-compatible libraries throughout (Ktor, Room, Koin, kotlinx.serialization)

## Non-Goals

- Replacing Volley for LAN host communication (flagged for future KMP migration)
- Modifying the legacy Fragment-based screens
- Offline-first write queue with guaranteed delivery (best-effort sync only)

---

## Architecture

### Backend Changes

**Remove `description` field across the stack:**

- **Video entity**: remove `description` field
- **Liquibase**: new changelog to drop `description` column from `videos` table
- **VideoInputDto / VideoOutputDto**: remove `description` field
- **VideoMapper**: remove description mapping
- **Frontend VideoOutput interface**: remove `description` field
- **Frontend history page**: remove description from cards and search filter (search title only)

### Data Layer (Android)

**Room Entity — `VideoEntity`:**

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String (UUID)` | Primary key, from backend |
| `title` | `String` | Max 128 chars |
| `url` | `String` | YouTube URL |
| `thumbnailUrl` | `String` | Thumbnail URL |
| `createdAt` | `Long` | Epoch millis, for sorting |
| `synced` | `Boolean` | `true` if confirmed on backend |

**VideoDao:**

- `getAllVideos(): Flow<List<VideoEntity>>` — ordered by `createdAt` desc
- `searchVideos(query: String): Flow<List<VideoEntity>>` — LIKE search on title
- `insertAll(videos: List<VideoEntity>)` — upsert from backend
- `insert(video: VideoEntity)` — single insert
- `deleteById(id: String)` — individual delete
- `deleteAll()` — bulk delete
- `getUnsyncedVideos(): List<VideoEntity>` — for retry sync

### Networking Layer (Ktor Client)

**HttpClient configuration:**

- Engine: CIO (KMP-compatible)
- Content negotiation: kotlinx.serialization
- Auth plugin: injects Firebase Bearer token via HttpClient plugin
- Base URL: hardcoded constant pointing to remote backend
- Coexists with Volley (Volley handles LAN host, Ktor handles backend)

**DTOs:**

```kotlin
@Serializable
data class VideoOutputDto(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String
)

@Serializable
data class VideoInputDto(
    val title: String,
    val url: String,
    val thumbnailUrl: String
)
```

**VideoApiService:**

- `getAllVideos(): List<VideoOutputDto>` — GET `/videos/all`
- `getVideoById(id: String): VideoOutputDto` — GET `/videos/{id}`
- `createVideo(input: VideoInputDto): VideoOutputDto` — POST `/videos`
- `deleteVideo(id: String)` — DELETE `/videos/{id}`
- `deleteAllVideos()` — DELETE `/videos` (new endpoint, bulk delete for authenticated user)

**Error handling:**

- Network unreachable → fall back to Room cache silently
- 401 → redirect to login screen
- Other HTTP errors → Snackbar message

### Repository Layer

**VideoRepository** coordinates between `VideoApiService` and `VideoDao`:

- `getVideos(): Flow<List<VideoEntity>>` — returns Room Flow. Triggers background refresh from backend on collect. If backend unreachable, cached data is served. On fresh install (no `lastSyncTimestamp`), does a full pull before showing data.
- `saveVideo(input: VideoInputDto)` — POST to backend → save to Room with `synced = true`. If offline → save to Room with `synced = false`.
- `deleteVideo(id: String)` — DELETE on backend → remove from Room.
- `deleteAllVideos()` — calls a new backend endpoint `DELETE /videos` (bulk delete for the authenticated user) → clears Room.
- `syncUnsyncedVideos()` — pushes `synced = false` records to backend. Called on app start and when history screen opens.

**`lastSyncTimestamp`** stored in DataStore. When 0 or absent (new phone), triggers full pull from backend.

**New phone scenario:** User logs in → history screen opens → `lastSyncTimestamp` is 0 → full pull from backend → Room populated → UI shows complete history.

### Firebase Auth

**Setup:**

- Add Firebase Auth dependency and `google-services.json`
- Same Firebase project as frontend

**AuthRepository:**

- `signIn(email, password)` — Firebase email/password sign-in
- `signUp(email, password)` — Firebase registration
- `signOut()` — clears session
- `getCurrentUser(): FirebaseUser?`
- `getIdToken(): String?` — for API calls (Firebase SDK handles refresh)

**Auth UI:**

- Login/Register screen (Compose, MD3) — email + password fields, toggle between modes
- Shown when `getCurrentUser()` is null
- NavHost conditional start destination: authenticated → `"home"`, not → `"login"`

**Ktor integration:**

- Auth plugin calls `getIdToken()` before every backend request
- Attaches `Authorization: Bearer <token>` header
- On 401 response → redirect to login

### Dependency Injection (Koin)

Koin chosen over Hilt for KMP compatibility.

**Modules:**

- `networkModule` — `HttpClient`, `VideoApiService`
- `databaseModule` — `AppDatabase` (Room), `VideoDao`
- `repositoryModule` — `VideoRepository`, `AuthRepository`
- `viewModelModule` — `HistoryViewModel` (and future ViewModels)

**Integration:**

- Initialize Koin in custom `Application` class
- ViewModels via `koinViewModel()` in Compose
- Services/repos via constructor injection

---

## UI Design — History Screen

### Layout

```
┌─────────────────────────────┐
│  TopAppBar: "Watch History" │
│                          [⋮]│
├─────────────────────────────┤
│  🔍 Search bar              │
├─────────────────────────────┤
│  ┌─────────────────────────┐│
│  │ [Thumb] Title        [✕]││
│  │         URL             ││
│  └─────────────────────────┘│
│  ┌─────────────────────────┐│
│  │ [Thumb] Title        [✕]││
│  │         URL             ││
│  └─────────────────────────┘│
│         ...                 │
├─────────────────────────────┤
│  [Sort FAB]          (↑/↓) │
└─────────────────────────────┘
```

### Components

- **TopAppBar** (MD3): title "Watch History", overflow menu (⋮) with "Delete All" option (confirmation dialog)
- **Search bar**: MD3 OutlinedTextField with search icon, filters locally by title
- **Video cards**: MD3 Card — thumbnail left (~100dp, 16:9), title + URL stacked right, delete icon (✕) on far right. Swipe-to-delete as alternative gesture.
- **Sort FAB**: bottom-right, toggles asc/desc by date, persists in DataStore
- **Pull-to-refresh**: triggers backend sync
- **Empty states**:
  - No videos: centered icon + "No videos yet"
  - No search results: "No results for '{query}'"
  - Loading: centered CircularProgressIndicator
- **Color scheme**: red branding on FAB/accents, MD3 surface colors for cards

### HistoryViewModel

- `videos: StateFlow<List<VideoEntity>>` — from repository
- `searchQuery: StateFlow<String>`
- `isLoading: StateFlow<Boolean>`
- `sortDescending: StateFlow<Boolean>`
- Actions: `search()`, `deleteVideo()`, `deleteAll()`, `toggleSort()`, `refresh()`

---

## Video Sharing Integration

Revised flow when a video is shared to the LAN host:

1. User taps "Share" → video sent to LAN host via Volley (unchanged)
2. On success → fetch YouTube oEmbed metadata (unchanged)
3. On metadata received → `VideoRepository.saveVideo()`:
   - Creates `VideoInputDto` with title, url, thumbnailUrl
   - POST to backend → on success, save to Room with `synced = true`
   - If backend unreachable → save to Room with `synced = false`
4. History screen reflects new entry immediately via Room Flow

Titles longer than 128 chars are truncated client-side before sending.

Sync retry: on app start and history screen open, `syncUnsyncedVideos()` pushes `synced = false` records to backend.

---

## Dependencies to Add (Android)

| Library | Purpose | KMP-Ready |
|---------|---------|-----------|
| Ktor Client (CIO) | Backend HTTP | Yes |
| kotlinx.serialization | JSON | Yes |
| Room | Local cache | Yes |
| Koin | Dependency injection | Yes |
| Firebase Auth | Authentication | No (Android-specific, will need expect/actual in KMP) |
| DataStore Preferences | Settings/sync state | Yes |

---

## Future Work (Out of Scope)

- **Replace Volley with Ktor** for LAN host communication (KMP migration)
- **Migrate legacy Fragment screens** to Compose
- **Additional backend features** (pagination, server-side search)
