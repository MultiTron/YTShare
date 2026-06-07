package iliev.yt.share.mobile.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import iliev.yt.share.mobile.data.local.VideoDao
import iliev.yt.share.mobile.data.local.VideoEntity
import iliev.yt.share.mobile.data.remote.VideoApiService
import iliev.yt.share.mobile.data.remote.VideoInputDto
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
