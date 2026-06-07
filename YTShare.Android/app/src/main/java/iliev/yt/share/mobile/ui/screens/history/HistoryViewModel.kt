package iliev.yt.share.mobile.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iliev.yt.share.mobile.data.local.VideoEntity
import iliev.yt.share.mobile.data.repository.VideoRepository
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
