# Android History Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the Android app's history to the remote backend via Ktor, add Firebase Auth, replace raw SQLite with Room, and rebuild the history screen in Compose with MD3. Also remove the `description` field from the backend Video entity.

**Architecture:** Backend-first changes (drop `description`), then Android bottom-up: dependencies → Room data layer → Ktor networking → Repository → Firebase Auth → Koin DI → ViewModel → UI. The frontend gets a small patch to remove `description` references.

**Tech Stack:** Spring Boot 4 / Java 21 / Liquibase (backend), Angular 20+ (frontend), Kotlin / Jetpack Compose / Room / Ktor / Koin / Firebase Auth (Android)

---

### Task 1: Backend — Remove `description` from Video entity and DTOs

**Files:**
- Modify: `backend/src/main/java/iliev/yt/share/backend/video/Video.java:23-25`
- Modify: `backend/src/main/java/iliev/yt/share/backend/video/dto/VideoOutputDto.java:5-12`
- Modify: `backend/src/main/java/iliev/yt/share/backend/video/dto/VideoInputDto.java:3-9`
- Create: `backend/src/main/resources/db/changelog/30-05-changelog.yaml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`

- [ ] **Step 1: Create Liquibase changelog to drop the `description` column**

Create `backend/src/main/resources/db/changelog/30-05-changelog.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: 1748600000000-1
      author: iliev
      objectQuotingStrategy: QUOTE_ONLY_RESERVED_WORDS
      changes:
        - dropColumn:
            tableName: videos
            columnName: description
```

- [ ] **Step 2: Register the new changelog in the master file**

In `backend/src/main/resources/db/changelog/db.changelog-master.yaml`, add after the last `- include:` entry:

```yaml
  - include:
      file: db/changelog/30-05-changelog.yaml
```

The full file should be:

```yaml
databaseChangeLog:

  - include:
      file: db/changelog/13-01-changelog.yaml
  - include:
      file: db/changelog/26-01-changelog.xml
  - include:
      file: db/changelog/30-05-changelog.yaml
```

- [ ] **Step 3: Remove `description` from `Video.java`**

Remove these lines from `Video.java`:

```java
    @Column(length = 1024)
    private String description;
```

The entity should become:

```java
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "videos")
public class Video extends BaseEntity {
    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String thumbnailUrl;
}
```

- [ ] **Step 4: Remove `description` from `VideoOutputDto.java`**

```java
package iliev.yt.share.backend.video.dto;

import java.util.UUID;

public record VideoOutputDto(
        UUID id,
        String title,
        String url,
        String thumbnailUrl
) {
}
```

- [ ] **Step 5: Remove `description` from `VideoInputDto.java`**

```java
package iliev.yt.share.backend.video.dto;

public record VideoInputDto(
        String title,
        String url,
        String thumbnailUrl
) {
}
```

- [ ] **Step 6: Verify the `VideoMapper` requires no changes**

The mapper auto-maps by field name. Since `description` no longer exists on either side, MapStruct will simply stop mapping it. No code change needed in `VideoMapper.java`.

- [ ] **Step 7: Build and verify**

Run from `backend/`:

```bash
./mvnw clean compile
```

Expected: BUILD SUCCESS with no compilation errors.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/video/Video.java backend/src/main/java/iliev/yt/share/backend/video/dto/VideoOutputDto.java backend/src/main/java/iliev/yt/share/backend/video/dto/VideoInputDto.java backend/src/main/resources/db/changelog/30-05-changelog.yaml backend/src/main/resources/db/changelog/db.changelog-master.yaml
git commit -m "feat: remove description field from Video entity and DTOs"
```

---

### Task 2: Backend — Add bulk delete endpoint

**Files:**
- Modify: `backend/src/main/java/iliev/yt/share/backend/video/VideoController.java`
- Modify: `backend/src/main/java/iliev/yt/share/backend/video/VideoService.java`

- [ ] **Step 1: Add `deleteAllVideos()` to `VideoService.java`**

Add this method after the existing `deleteVideo` method:

```java
    @Transactional
    public void deleteAllVideos() {
        videoRepository.deleteAll();
    }
```

- [ ] **Step 2: Add `DELETE /videos` endpoint to `VideoController.java`**

Add this method after the existing `deleteVideo` method:

```java
    @DeleteMapping
    public void deleteAllVideos() {
        videoService.deleteAllVideos();
    }
```

- [ ] **Step 3: Build and verify**

Run from `backend/`:

```bash
./mvnw clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/iliev/yt/share/backend/video/VideoController.java backend/src/main/java/iliev/yt/share/backend/video/VideoService.java
git commit -m "feat: add bulk delete endpoint for videos"
```

---

### Task 3: Frontend — Remove `description` references

**Files:**
- Modify: `frontend/src/app/core/services/video.service.ts:6-12`
- Modify: `frontend/src/app/features/history/history-page.component.ts:42-53`
- Modify: `frontend/src/app/features/history/history-page.component.html:73-75`

- [ ] **Step 1: Remove `description` from `VideoOutput` interface**

In `frontend/src/app/core/services/video.service.ts`, change the interface to:

```typescript
export interface VideoOutput {
  id: string;
  title: string;
  url: string;
  thumbnailUrl: string;
}
```

- [ ] **Step 2: Remove `description` from search filter**

In `frontend/src/app/features/history/history-page.component.ts`, change the `onSearch()` method to:

```typescript
  onSearch(): void {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) {
      this.filteredVideos.set(this.videos());
      return;
    }
    this.filteredVideos.set(
      this.videos().filter(v =>
        v.title.toLowerCase().includes(q)
      )
    );
  }
```

- [ ] **Step 3: Remove description rendering from HTML**

In `frontend/src/app/features/history/history-page.component.html`, remove lines 73-75:

```html
                @if (video.description) {
                  <p class="video-description">{{ video.description }}</p>
                }
```

- [ ] **Step 4: Verify frontend builds**

Run from `frontend/`:

```bash
npm run build
```

Expected: Build succeeds with no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/services/video.service.ts frontend/src/app/features/history/history-page.component.ts frontend/src/app/features/history/history-page.component.html
git commit -m "feat: remove description field from frontend video model and history page"
```

---

### Task 4: Android — Add new dependencies to version catalog and build files

**Files:**
- Modify: `YTShare.Android/gradle/libs.versions.toml`
- Modify: `YTShare.Android/app/build.gradle.kts`
- Modify: `YTShare.Android/build.gradle.kts`
- Modify: `YTShare.Android/settings.gradle.kts`

- [ ] **Step 1: Add versions to `libs.versions.toml`**

Add these entries to the `[versions]` section:

```toml
ktor = "3.1.3"
room = "2.7.1"
ksp = "2.2.21-2.0.1"
koin = "4.1.0"
kotlinxSerialization = "1.8.1"
datastore = "1.1.7"
firebaseBom = "33.15.0"
googleServices = "4.4.2"
kotlinxCoroutinesPlayServices = "1.10.2"
```

Add these entries to the `[libraries]` section:

```toml
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# Koin
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# DataStore
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# Firebase
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { module = "com.google.firebase:firebase-auth-ktx" }

# Coroutines
kotlinx-coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutinesPlayServices" }
```

Add these entries to the `[plugins]` section:

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Update project-level `build.gradle.kts`**

Replace the content of `YTShare.Android/build.gradle.kts` with:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Update app-level `build.gradle.kts`**

In `YTShare.Android/app/build.gradle.kts`, add the new plugins:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}
```

Add the new dependencies to the `dependencies` block:

```kotlin
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)
```

- [ ] **Step 4: Add google-services.json**

Copy the Firebase `google-services.json` file for the Android app from the Firebase console and place it at:

```
YTShare.Android/app/google-services.json
```

This must be done manually — it contains project-specific Firebase credentials.

- [ ] **Step 5: Sync Gradle and verify**

Run from `YTShare.Android/`:

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESS (Firebase will warn if `google-services.json` is missing — that's OK for now, it's needed before the auth step).

- [ ] **Step 6: Commit**

```bash
git add YTShare.Android/gradle/libs.versions.toml YTShare.Android/app/build.gradle.kts YTShare.Android/build.gradle.kts
git commit -m "feat: add Ktor, Room, Koin, Firebase, DataStore dependencies"
```

---

### Task 5: Android — Room data layer (VideoEntity + VideoDao + AppDatabase)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/VideoEntity.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/VideoDao.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/AppDatabase.kt`

- [ ] **Step 1: Create `VideoEntity.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/VideoEntity.kt`:

```kotlin
package com.example.ytshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val createdAt: Long,
    val synced: Boolean = true
)
```

- [ ] **Step 2: Create `VideoDao.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/VideoDao.kt`:

```kotlin
package com.example.ytshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()

    @Query("SELECT * FROM videos WHERE synced = 0")
    suspend fun getUnsyncedVideos(): List<VideoEntity>
}
```

- [ ] **Step 3: Create `AppDatabase.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/local/AppDatabase.kt`:

```kotlin
package com.example.ytshare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VideoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}
```

- [ ] **Step 4: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/local/
git commit -m "feat: add Room data layer with VideoEntity, VideoDao, and AppDatabase"
```

---

### Task 6: Android — Ktor networking layer (DTOs + VideoApiService)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoOutputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoInputDto.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoApiService.kt`
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/Constants.kt`

- [ ] **Step 1: Add backend base URL to `Constants.kt`**

Add the backend URL constant to `Constants.kt`:

```kotlin
package com.example.ytshare

object Constants {
    const val isTracking = "isTracking"
    const val ip = "ip"
    const val link = "link"
    const val isHistoryDesc = "isHistoryDesc"

    const val BACKEND_BASE_URL = "https://your-backend-server.com/api"
}
```

> **Note:** Replace `https://your-backend-server.com/api` with the actual backend URL before deploying.

- [ ] **Step 2: Create `VideoOutputDto.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoOutputDto.kt`:

```kotlin
package com.example.ytshare.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class VideoOutputDto(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String
)
```

- [ ] **Step 3: Create `VideoInputDto.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoInputDto.kt`:

```kotlin
package com.example.ytshare.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class VideoInputDto(
    val title: String,
    val url: String,
    val thumbnailUrl: String
)
```

- [ ] **Step 4: Create `VideoApiService.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/VideoApiService.kt`:

```kotlin
package com.example.ytshare.data.remote

import com.example.ytshare.Constants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class VideoApiService(private val client: HttpClient) {

    private val baseUrl = "${Constants.BACKEND_BASE_URL}/videos"

    suspend fun getAllVideos(): List<VideoOutputDto> {
        return client.get("$baseUrl/all").body()
    }

    suspend fun getVideoById(id: String): VideoOutputDto {
        return client.get("$baseUrl/$id").body()
    }

    suspend fun createVideo(input: VideoInputDto): VideoOutputDto {
        return client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    suspend fun deleteVideo(id: String) {
        client.delete("$baseUrl/$id")
    }

    suspend fun deleteAllVideos() {
        client.delete(baseUrl)
    }
}
```

- [ ] **Step 5: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/remote/ YTShare.Android/app/src/main/java/com/example/ytshare/Constants.kt
git commit -m "feat: add Ktor networking layer with VideoApiService and DTOs"
```

---

### Task 7: Android — Firebase Auth (AuthRepository)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/auth/AuthRepository.kt`

- [ ] **Step 1: Create `AuthRepository.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/auth/AuthRepository.kt`:

```kotlin
package com.example.ytshare.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/auth/
git commit -m "feat: add Firebase AuthRepository"
```

---

### Task 8: Android — VideoRepository (coordinates API + Room)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/VideoRepository.kt`

- [ ] **Step 1: Create `VideoRepository.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/VideoRepository.kt`:

```kotlin
package com.example.ytshare.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.ytshare.data.local.VideoDao
import com.example.ytshare.data.local.VideoEntity
import com.example.ytshare.data.remote.VideoApiService
import com.example.ytshare.data.remote.VideoInputDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VideoRepository(
    private val api: VideoApiService,
    private val dao: VideoDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "VideoRepository"
        private val LAST_SYNC_KEY = longPreferencesKey("last_sync_timestamp")
    }

    fun getVideos(): Flow<List<VideoEntity>> {
        return dao.getAllVideos()
    }

    fun searchVideos(query: String): Flow<List<VideoEntity>> {
        return dao.searchVideos(query)
    }

    suspend fun refreshFromBackend() {
        try {
            val remoteVideos = api.getAllVideos()
            val entities = remoteVideos.map { dto ->
                VideoEntity(
                    id = dto.id,
                    title = dto.title,
                    url = dto.url,
                    thumbnailUrl = dto.thumbnailUrl,
                    createdAt = System.currentTimeMillis(),
                    synced = true
                )
            }
            dao.deleteAll()
            dao.insertAll(entities)
            updateLastSync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh from backend", e)
        }
    }

    suspend fun saveVideo(input: VideoInputDto) {
        try {
            val response = api.createVideo(input)
            val entity = VideoEntity(
                id = response.id,
                title = response.title,
                url = response.url,
                thumbnailUrl = response.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
                synced = true
            )
            dao.insert(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to backend, caching locally", e)
            val entity = VideoEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = input.title,
                url = input.url,
                thumbnailUrl = input.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
                synced = false
            )
            dao.insert(entity)
        }
    }

    suspend fun deleteVideo(id: String) {
        try {
            api.deleteVideo(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from backend", e)
        }
        dao.deleteById(id)
    }

    suspend fun deleteAllVideos() {
        try {
            api.deleteAllVideos()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all from backend", e)
        }
        dao.deleteAll()
    }

    suspend fun syncUnsyncedVideos() {
        val unsynced = dao.getUnsyncedVideos()
        for (video in unsynced) {
            try {
                val input = VideoInputDto(
                    title = video.title,
                    url = video.url,
                    thumbnailUrl = video.thumbnailUrl
                )
                val response = api.createVideo(input)
                dao.deleteById(video.id)
                val synced = VideoEntity(
                    id = response.id,
                    title = response.title,
                    url = response.url,
                    thumbnailUrl = response.thumbnailUrl,
                    createdAt = video.createdAt,
                    synced = true
                )
                dao.insert(synced)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync video ${video.id}", e)
                break
            }
        }
    }

    suspend fun needsFullSync(): Boolean {
        val lastSync = dataStore.data.map { prefs ->
            prefs[LAST_SYNC_KEY] ?: 0L
        }.first()
        return lastSync == 0L
    }

    private suspend fun updateLastSync() {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_KEY] = System.currentTimeMillis()
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/data/repository/
git commit -m "feat: add VideoRepository coordinating backend API and Room cache"
```

---

### Task 9: Android — Koin dependency injection setup

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/YTShareApplication.kt`
- Modify: `YTShare.Android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `AppModules.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt`:

```kotlin
package com.example.ytshare.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.ytshare.data.auth.AuthRepository
import com.example.ytshare.data.local.AppDatabase
import com.example.ytshare.data.remote.VideoApiService
import com.example.ytshare.data.repository.VideoRepository
import com.example.ytshare.ui.screens.history.HistoryViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ytshare_prefs")

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "ytshare-db"
        ).build()
    }
    single { get<AppDatabase>().videoDao() }
}

val networkModule = module {
    single {
        val authRepository = get<AuthRepository>()
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                val token = runBlocking { authRepository.getIdToken() }
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }
    single { VideoApiService(get()) }
}

val repositoryModule = module {
    single { AuthRepository() }
    single { VideoRepository(get(), get(), androidContext().dataStore) }
}

val viewModelModule = module {
    viewModel { HistoryViewModel(get()) }
}

val appModules = listOf(databaseModule, networkModule, repositoryModule, viewModelModule)
```

- [ ] **Step 2: Create `YTShareApplication.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/YTShareApplication.kt`:

```kotlin
package com.example.ytshare

import android.app.Application
import com.example.ytshare.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YTShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YTShareApplication)
            modules(appModules)
        }
    }
}
```

- [ ] **Step 3: Register the Application class in `AndroidManifest.xml`**

In `YTShare.Android/app/src/main/AndroidManifest.xml`, add `android:name=".YTShareApplication"` to the `<application>` tag:

```xml
    <application
        android:name=".YTShareApplication"
        android:allowBackup="true"
```

- [ ] **Step 4: Verify compilation**

This step will fail until `HistoryViewModel` exists (Task 10). That's expected — the Koin module references it. You can either comment out the `viewModelModule` temporarily or proceed to Task 10 first.

- [ ] **Step 5: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/di/ YTShare.Android/app/src/main/java/com/example/ytshare/YTShareApplication.kt YTShare.Android/app/src/main/AndroidManifest.xml
git commit -m "feat: add Koin DI with network, database, and repository modules"
```

---

### Task 10: Android — HistoryViewModel

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/HistoryViewModel.kt`

- [ ] **Step 1: Create `HistoryViewModel.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/HistoryViewModel.kt`:

```kotlin
package com.example.ytshare.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.data.local.VideoEntity
import com.example.ytshare.data.repository.VideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortDescending = MutableStateFlow(true)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val videos: StateFlow<List<VideoEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getVideos()
            } else {
                repository.searchVideos(query)
            }
        }
        .map { videos ->
            if (_sortDescending.value) {
                videos.sortedByDescending { it.createdAt }
            } else {
                videos.sortedBy { it.createdAt }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun toggleSort() {
        _sortDescending.value = !_sortDescending.value
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            repository.deleteVideo(id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAllVideos()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncUnsyncedVideos()
            repository.refreshFromBackend()
            _isLoading.value = false
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/
git commit -m "feat: add HistoryViewModel with search, sort, delete, and sync"
```

---

### Task 11: Android — Redesigned History Screen (Compose + MD3)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/HistoryScreen.kt` (replaces the old `ui/screens/HistoryScreen.kt`)
- Delete or leave: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HistoryScreen.kt` (old file)

- [ ] **Step 1: Create the new `HistoryScreen.kt` in the history package**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/HistoryScreen.kt`:

```kotlin
package com.example.ytshare.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ytshare.data.local.VideoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val videos by viewModel.videos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortDescending by viewModel.sortDescending.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch History") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete All") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleSort() },
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = "Toggle sort"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search videos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading && videos.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    videos.isEmpty() && searchQuery.isNotEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No results",
                            subtitle = "No videos match \"$searchQuery\""
                        )
                    }
                    videos.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.PlayCircle,
                            title = "No videos yet",
                            subtitle = "Videos shared through YTShare will appear here"
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(videos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    onDelete = { viewModel.deleteVideo(video.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All History") },
            text = { Text("Are you sure you want to delete all video history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VideoCard(
    video: VideoEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: Delete the old `HistoryScreen.kt`**

Delete `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HistoryScreen.kt`.

- [ ] **Step 3: Verify compilation**

Run from `YTShare.Android/`:

```bash
./gradlew compileDebugKotlin
```

Expected: Will fail because `MainActivityCompose.kt` still imports the old `HistoryScreen`. That's fixed in Task 13.

- [ ] **Step 4: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/history/HistoryScreen.kt
git rm YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HistoryScreen.kt
git commit -m "feat: redesigned history screen with MD3, search, sort, pull-to-refresh"
```

---

### Task 12: Android — Login Screen (Compose + MD3)

**Files:**
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/auth/AuthViewModel.kt`
- Create: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/auth/LoginScreen.kt`

- [ ] **Step 1: Create `AuthViewModel.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/auth/AuthViewModel.kt`:

```kotlin
package com.example.ytshare.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytshare.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(authRepository.isAuthenticated)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _isAuthenticated.value = true
            }.onFailure { e ->
                _error.value = e.message ?: "Sign in failed"
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signUp(email, password)
            result.onSuccess {
                _isAuthenticated.value = true
            }.onFailure { e ->
                _error.value = e.message ?: "Sign up failed"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isAuthenticated.value = false
    }
}
```

- [ ] **Step 2: Create `LoginScreen.kt`**

Create `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/auth/LoginScreen.kt`:

```kotlin
package com.example.ytshare.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "YTShare",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRegisterMode) "Create an account" else "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isRegisterMode) {
                    viewModel.signUp(email, password)
                } else {
                    viewModel.signIn(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = Color.White
                )
            } else {
                Text(if (isRegisterMode) "Register" else "Sign In")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(
                if (isRegisterMode) "Already have an account? Sign in"
                else "Don't have an account? Register"
            )
        }
    }
}
```

- [ ] **Step 3: Register `AuthViewModel` in Koin**

In `YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt`, add the import and viewModel registration:

Add import:

```kotlin
import com.example.ytshare.ui.screens.auth.AuthViewModel
```

Update `viewModelModule`:

```kotlin
val viewModelModule = module {
    viewModel { HistoryViewModel(get()) }
    viewModel { AuthViewModel(get()) }
}
```

- [ ] **Step 4: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/auth/ YTShare.Android/app/src/main/java/com/example/ytshare/di/AppModules.kt
git commit -m "feat: add login/register screen with AuthViewModel"
```

---

### Task 13: Android — Wire everything into MainActivityCompose + Navigation

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt`

- [ ] **Step 1: Rewrite `MainActivityCompose.kt`**

Replace the full content of `MainActivityCompose.kt` with:

```kotlin
package com.example.ytshare

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.ytshare.helpers.DBHelper
import com.example.ytshare.helpers.NSDHelper
import com.example.ytshare.helpers.SharedPrefHelper
import com.example.ytshare.ui.screens.HomeScreen
import com.example.ytshare.ui.screens.SettingsScreen
import com.example.ytshare.ui.screens.auth.AuthViewModel
import com.example.ytshare.ui.screens.auth.LoginScreen
import com.example.ytshare.ui.screens.history.HistoryScreen
import com.example.ytshare.ui.screens.history.HistoryViewModel
import com.example.ytshare.ui.theme.YTShareTheme
import org.koin.androidx.compose.koinViewModel

class MainActivityCompose : ComponentActivity() {

    lateinit var queue: RequestQueue
    lateinit var sharedPref: SharedPreferences
    lateinit var db: DBHelper
    lateinit var nsd: NSDHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initializeNSD()
        db = DBHelper(this, null)
        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }

        setContent {
            YTShareTheme {
                val authViewModel: AuthViewModel = koinViewModel()
                val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

                if (isAuthenticated) {
                    MainScreen(authViewModel)
                } else {
                    LoginScreen(viewModel = authViewModel)
                }
            }
        }
    }

    @Composable
    fun MainScreen(authViewModel: AuthViewModel) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var ipAddress by remember { mutableStateOf(sharedPref.getString(Constants.ip, "0.0.0.0") ?: "0.0.0.0") }
        var savedLink by remember { mutableStateOf(sharedPref.getString(Constants.link, "")) }
        var isTracking by remember { mutableStateOf(sharedPref.getBoolean(Constants.isTracking, false)) }
        var hosts by remember { mutableStateOf(nsd.addresses) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(Dp(30f))
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedTextColor = Color.White,
                                indicatorColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeScreen(
                        savedLink = savedLink,
                        ipAddress = ipAddress,
                        isTracking = isTracking,
                        queue = queue,
                        db = db,
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onClearLink = {
                            SharedPrefHelper.clearLink(sharedPref)
                            savedLink = ""
                        }
                    )
                }

                composable("history") {
                    val historyViewModel: HistoryViewModel = koinViewModel()
                    HistoryScreen(viewModel = historyViewModel)
                }

                composable("settings") {
                    LaunchedEffect(Unit) {
                        hosts = nsd.addresses
                    }

                    SettingsScreen(
                        hosts = hosts,
                        isTrackingEnabled = isTracking,
                        onHostSelected = { host ->
                            val hostString = host.toString()
                            SharedPrefHelper.saveIp(hostString, sharedPref)
                            ipAddress = hostString
                        },
                        onTrackingChanged = { enabled ->
                            isTracking = enabled
                            SharedPrefHelper.savePref(enabled, sharedPref)
                        }
                    )
                }
            }
        }
    }

    private fun initializeNSD() {
        nsd = NSDHelper(this)
        nsd.discoverServices()

        if (nsd.addresses.isNotEmpty()) {
            val host = nsd.addresses.first()
            if (host.address.isNotEmpty()) {
                SharedPrefHelper.saveIp(host.toString(), sharedPref)
            } else {
                SharedPrefHelper.clearIp(sharedPref)
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            SharedPrefHelper.saveLink(it, sharedPref)
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Filled.Home, "Home"),
    BottomNavItem("history", Icons.Filled.History, "History"),
    BottomNavItem("settings", Icons.Filled.Settings, "Settings")
)
```

- [ ] **Step 2: Build the full project**

Run from `YTShare.Android/`:

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/MainActivityCompose.kt
git commit -m "feat: wire auth, history ViewModel, and Koin into navigation"
```

---

### Task 14: Android — Integrate video saving with backend on share

**Files:**
- Modify: `YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Update `HomeScreen.kt` to save via VideoRepository**

The `saveLinkInfo` function currently saves directly to SQLite via `DBHelper`. Change it to use `VideoRepository` instead. The challenge is that `saveLinkInfo` runs inside a Volley callback (not a coroutine), so we need to launch a coroutine.

Replace the `saveLinkInfo` function and update the `shareRequest` function:

```kotlin
package com.example.ytshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.ytshare.R
import com.example.ytshare.data.remote.VideoInputDto
import com.example.ytshare.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    savedLink: String?,
    ipAddress: String,
    isTracking: Boolean,
    queue: RequestQueue,
    db: com.example.ytshare.helpers.DBHelper,
    onNavigateToSettings: () -> Unit,
    onClearLink: () -> Unit
) {
    val context = LocalContext.current
    val videoRepository: VideoRepository = koinInject()
    var urlText by remember { mutableStateOf(modifyLink(savedLink, isTracking)) }
    var isLoading by remember { mutableStateOf(false) }
    val baseAddress = "http://$ipAddress/Share?link="

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colorResource(id = R.color.red),
                                colorResource(id = R.color.dark_red)
                            )
                        ),
                        shape = RoundedCornerShape(
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
            )
            Text(
                text = ipAddress,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 25.dp)
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Enter URL") },
            placeholder = { Text("URL Hint") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp)
        )

        Button(
            onClick = {
                if (urlText.isNotEmpty()) {
                    isLoading = true
                    shareRequest(
                        link = urlText,
                        baseAddress = baseAddress,
                        queue = queue,
                        videoRepository = videoRepository,
                        context = context,
                        onSuccess = {
                            isLoading = false
                            urlText = ""
                            onClearLink()
                        },
                        onError = {
                            isLoading = false
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Share",
                fontSize = 35.sp
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(100.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(48.dp)
            )
        }
    }
}

private fun modifyLink(url: String?, isTracking: Boolean): String {
    val protocolRegex = Regex("""^(https?://)""")
    val trackingRegex = Regex("""[\?&]si=[^&]+|[\?&]t=[^&]+""")
    val remainsRegex = Regex("""[?&]$""")

    val withoutProtocol = url?.replace(protocolRegex, "") ?: ""

    return if (isTracking) {
        withoutProtocol.replace(trackingRegex, "").replace(remainsRegex, "")
    } else {
        withoutProtocol
    }
}

private fun shareRequest(
    link: String,
    baseAddress: String,
    queue: RequestQueue,
    videoRepository: VideoRepository,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val stringRequest = StringRequest(
        Request.Method.GET, "$baseAddress$link",
        { response ->
            saveLinkInfo(link, queue, videoRepository)
            Toast.makeText(context, response, Toast.LENGTH_LONG).show()
            onSuccess()
        },
        {
            Toast.makeText(
                context,
                "Could not establish connection with server",
                Toast.LENGTH_SHORT
            ).show()
            onError()
        })

    queue.add(stringRequest)
}

private fun saveLinkInfo(link: String, queue: RequestQueue, videoRepository: VideoRepository) {
    val stringRequest = StringRequest(
        Request.Method.GET, "https://www.youtube.com/oembed?url=$link&format=json",
        { response ->
            val json = JSONObject(response)
            val title = json.get("title").toString().take(128)
            val thumbnailUrl = json.get("thumbnail_url").toString()

            CoroutineScope(Dispatchers.IO).launch {
                videoRepository.saveVideo(
                    VideoInputDto(
                        title = title,
                        url = link,
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }
        },
        {
            android.util.Log.e("LinkInfo", "Unable to reach YouTube Info Server...")
        })

    queue.add(stringRequest)
}
```

- [ ] **Step 2: Note about `db` parameter**

The `db: DBHelper` parameter is still in the `HomeScreen` signature for backward compatibility (other code may reference it). It's no longer used for saving history. It can be removed in a future cleanup when the rest of the app is migrated.

- [ ] **Step 3: Build and verify**

Run from `YTShare.Android/`:

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add YTShare.Android/app/src/main/java/com/example/ytshare/ui/screens/HomeScreen.kt
git commit -m "feat: save shared videos via VideoRepository instead of direct SQLite"
```

---

### Task 15: End-to-end verification

- [ ] **Step 1: Start the backend**

From `backend/`:

```bash
./mvnw spring-boot:run
```

Verify it starts without errors. The Liquibase migration should drop the `description` column.

- [ ] **Step 2: Verify the frontend builds and runs**

From `frontend/`:

```bash
npm start
```

Navigate to the history page. Verify no errors in console, cards render without description.

- [ ] **Step 3: Run the Android app**

Build and deploy to an emulator or device. Verify:

1. Login screen appears on first launch
2. Can register/sign in with email and password
3. After auth, main screen shows with bottom navigation
4. Share a YouTube link — it sends to the LAN host and appears in history
5. History screen shows video cards with thumbnail, title, URL
6. Search filters videos by title
7. Sort FAB toggles ascending/descending
8. Individual delete removes a single video
9. Overflow menu → Delete All removes all videos (with confirmation)
10. Pull-to-refresh syncs from backend

- [ ] **Step 4: Verify new phone scenario**

Sign in on a different emulator (simulating new phone). History should populate from the backend.

- [ ] **Step 5: Final commit if any adjustments were needed**

```bash
git add -A
git commit -m "fix: adjustments from end-to-end testing"
```
