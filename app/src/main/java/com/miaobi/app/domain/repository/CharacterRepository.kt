package com.miaobi.app.domain.repository

import com.miaobi.app.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getCharactersByStoryId(storyId: Long): Flow<List<Character>>
    suspend fun getCharacterById(characterId: Long): Character?
    suspend fun insertCharacter(character: Character): Long
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(characterId: Long)
}
