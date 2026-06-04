package com.memoria.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.memoria.app.data.model.*
import com.memoria.app.data.repository.MemorIADatabase
import com.memoria.app.data.repository.MemoryRepository
import com.memoria.app.utils.ImageProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "MemoryViewModel"

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MemoryRepository

    init {
        val db = MemorIADatabase.getDatabase(application)
        repository = MemoryRepository(db.memoryDao())
        Log.d(TAG, "ViewModel initialized")
    }

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _imageProcessState = MutableStateFlow(ImageProcessState())
    val imageProcessState: StateFlow<ImageProcessState> = _imageProcessState.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    val memories: StateFlow<List<Memory>> = combine(
        repository.allMemories,
        repository.favoriteMemories,
        _searchQuery,
        _showFavoritesOnly
    ) { all, favorites, query, favOnly ->
        val src = if (favOnly) favorites else all
        if (query.isBlank()) src
        else src.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    }.catch { e ->
        Log.e(TAG, "Error loading memories", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Pair<Int, Int>> = combine(
        repository.allMemories,
        repository.favoriteMemories
    ) { all, favorites ->
        Pair(all.size, favorites.size)
    }.catch { emit(Pair(0, 0)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Pair(0, 0))

    fun saveMemory(
        title: String,
        description: String = "",
        imagePath: String? = null,
        videoPath: String? = null,
        emotionTag: EmotionTag = EmotionTag.JOY,
        location: String = "",
        processedImagePath: String? = null
    ) {
        if (title.isBlank()) { _uiState.update { it.copy(error = "El título no puede estar vacío") }; return }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val type = when {
                    imagePath != null && videoPath != null -> MemoryType.MIXED
                    videoPath != null -> MemoryType.VIDEO
                    else -> MemoryType.PHOTO
                }
                val id = repository.insertMemory(
                    Memory(
                        title              = title.trim(),
                        description        = description.trim(),
                        imagePath          = imagePath,
                        processedImagePath = processedImagePath,
                        videoPath          = videoPath,
                        type               = type,
                        emotionTag         = emotionTag,
                        location           = location
                    )
                )
                Log.d(TAG, "Memory saved id=$id")
                _uiState.update { it.copy(isLoading = false) }
                _imageProcessState.update { it.copy(savedMemoryId = id) }
            } catch (e: Exception) {
                Log.e(TAG, "Save failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            try { repository.deleteMemory(memory) }
            catch (e: Exception) { Log.e(TAG, "Delete failed", e) }
        }
    }

    fun toggleFavorite(memory: Memory) {
        viewModelScope.launch {
            try { repository.toggleFavorite(memory.id, !memory.isFavorite) }
            catch (e: Exception) { Log.e(TAG, "Toggle fav failed", e) }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun toggleFavoritesFilter() { _showFavoritesOnly.update { !it } }
    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun setSourceImage(path: String) {
        _imageProcessState.value = ImageProcessState(originalPath = path)
    }

    fun resetImageProcess() {
        val orig = _imageProcessState.value.originalPath
        _imageProcessState.value = ImageProcessState(originalPath = orig)
    }

    fun processImage(type: ProcessingType, format: ImageFormat, quality: Int = 85) {
        val src = _imageProcessState.value.originalPath ?: return
        viewModelScope.launch {
            _imageProcessState.update { it.copy(isProcessing = true, error = null, processingType = type, outputFormat = format, quality = quality) }
            ImageProcessor.processImage(getApplication(), src, type, format, quality).fold(
                onSuccess = { path -> _imageProcessState.update { it.copy(isProcessing = false, processedPath = path) } },
                onFailure = { e   -> _imageProcessState.update { it.copy(isProcessing = false, error = e.message) } }
            )
        }
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return
        _timerState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (isActive) { delay(1_000); _timerState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) } }
        }
    }

    fun pauseTimer() { timerJob?.cancel(); _timerState.update { it.copy(isRunning = false) } }
    fun resetTimer() { timerJob?.cancel(); _timerState.value = TimerState() }
    fun addLap()     { val t = _timerState.value.elapsedSeconds; _timerState.update { it.copy(laps = it.laps + t) } }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        Log.d(TAG, "ViewModel cleared")
    }
}

class MemoryViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(cls: Class<T>): T {
        if (cls.isAssignableFrom(MemoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MemoryViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${cls.name}")
    }
}