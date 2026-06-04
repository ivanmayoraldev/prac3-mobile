package com.memoria.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryType { PHOTO, VIDEO, MIXED }

enum class EmotionTag { JOY, LOVE, NOSTALGIA, ADVENTURE, PEACE, SURPRISE, GRATITUDE }

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val imagePath: String? = null,
    val processedImagePath: String? = null,
    val videoPath: String? = null,
    val type: MemoryType = MemoryType.PHOTO,
    val emotionTag: EmotionTag = EmotionTag.JOY,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val capturedAt: Long = System.currentTimeMillis()
)

data class MemoryUiState(
    val memories: List<Memory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CameraUiState(
    val capturedImagePath: String? = null,
    val hasPermission: Boolean = false,
    val error: String? = null
)

data class ImageProcessState(
    val originalPath: String? = null,
    val processedPath: String? = null,
    val isProcessing: Boolean = false,
    val processingType: ProcessingType = ProcessingType.NONE,
    val outputFormat: ImageFormat = ImageFormat.JPEG,
    val quality: Int = 85,
    val error: String? = null,
    val savedMemoryId: Long? = null
)

enum class ProcessingType(val label: String, val emoji: String) {
    NONE("Original",    "🖼️"),
    GRAYSCALE("Grises",   "🌫️"),
    SEPIA("Sepia",     "☕"),
    VINTAGE("Vintage",   "📷"),
    WARM("Cálido",    "🌅"),
    COOL("Frío",      "❄️"),
    BRIGHTNESS("Brillo",    "☀️"),
    CONTRAST("Contraste", "◐"),
    ROTATE_90("Rotar 90°", "🔄"),
    FLIP_H("Voltear",   "↔️"),
    COMPRESS("Comprimir", "🗜️")
}

enum class ImageFormat(val label: String, val ext: String) {
    JPEG("JPEG", "jpg"),
    PNG("PNG", "png"),
    WEBP("WEBP", "webp")
}

data class TimerState(
    val elapsedSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)