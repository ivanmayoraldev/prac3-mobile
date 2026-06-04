package com.memoria.app.data.repository

import android.content.Context
import androidx.room.*
import com.memoria.app.data.model.Memory
import com.memoria.app.data.model.EmotionTag
import com.memoria.app.data.model.MemoryType
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = MemoryType.valueOf(value)

    @TypeConverter
    fun fromEmotionTag(tag: EmotionTag): String = tag.name

    @TypeConverter
    fun toEmotionTag(value: String): EmotionTag = EmotionTag.valueOf(value)
}

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): Memory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("UPDATE memories SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int

    @Query("SELECT COUNT(*) FROM memories WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
}

@Database(
    entities = [Memory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MemorIADatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemorIADatabase? = null

        fun getDatabase(context: Context): MemorIADatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemorIADatabase::class.java,
                    "memoria_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MemoryRepository(private val dao: MemoryDao) {

    val allMemories: Flow<List<Memory>> = dao.getAllMemories()
    val favoriteMemories: Flow<List<Memory>> = dao.getFavoriteMemories()

    fun searchMemories(query: String): Flow<List<Memory>> = dao.searchMemories(query)

    suspend fun getMemoryById(id: Long): Memory? = dao.getMemoryById(id)

    suspend fun insertMemory(memory: Memory): Long = dao.insertMemory(memory)

    suspend fun updateMemory(memory: Memory) = dao.updateMemory(memory)

    suspend fun deleteMemory(memory: Memory) = dao.deleteMemory(memory)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        dao.updateFavorite(id, isFavorite)

    suspend fun getStats(): Pair<Int, Int> {
        val total = dao.getMemoryCount()
        val favorites = dao.getFavoriteCount()
        return Pair(total, favorites)
    }
}
