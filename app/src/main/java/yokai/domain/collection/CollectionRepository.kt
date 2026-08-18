package yokai.domain.collection

import eu.kanade.tachiyomi.data.database.models.LibraryCollection
import kotlinx.coroutines.flow.Flow
import yokai.domain.collection.models.CollectionUpdate

interface CollectionRepository {
    suspend fun getAll(): List<LibraryCollection>
    fun getAllAsFlow(): Flow<List<LibraryCollection>>
    suspend fun getById(id: Long): LibraryCollection?
    suspend fun insert(collection: LibraryCollection): Long?
    suspend fun update(update: CollectionUpdate): Boolean
    suspend fun delete(id: Long)
    suspend fun addManga(collectionId: Long, mangaId: Long)
    suspend fun removeManga(collectionId: Long, mangaId: Long)
    suspend fun getMangaIds(collectionId: Long): List<Long>
    fun getMangaIdsAsFlow(collectionId: Long): Flow<List<Long>>
    suspend fun getCollectionIdsByMangaId(mangaId: Long): List<Long>
    suspend fun getMangaCount(collectionId: Long): Long
}
