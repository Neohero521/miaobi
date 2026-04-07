package com.miaobi.app.data.local.dao

import androidx.room.*
import com.miaobi.app.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE storyId = :storyId ORDER BY createdAt ASC")
    fun getCharactersByStoryId(storyId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :characterId")
    suspend fun getCharacterById(characterId: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :characterId")
    suspend fun deleteCharacterById(characterId: Long)
}
