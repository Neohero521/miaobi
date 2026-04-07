package com.miaobi.app.data.repository

import com.miaobi.app.data.local.dao.CharacterDao
import com.miaobi.app.domain.model.Character
import com.miaobi.app.domain.repository.CharacterRepository
import com.miaobi.app.util.toDomain
import com.miaobi.app.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao
) : CharacterRepository {

    override fun getCharactersByStoryId(storyId: Long): Flow<List<Character>> {
        return characterDao.getCharactersByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCharacterById(characterId: Long): Character? {
        return characterDao.getCharacterById(characterId)?.toDomain()
    }

    override suspend fun insertCharacter(character: Character): Long {
        return characterDao.insertCharacter(character.toEntity())
    }

    override suspend fun updateCharacter(character: Character) {
        characterDao.updateCharacter(character.toEntity())
    }

    override suspend fun deleteCharacter(characterId: Long) {
        characterDao.deleteCharacterById(characterId)
    }
}
