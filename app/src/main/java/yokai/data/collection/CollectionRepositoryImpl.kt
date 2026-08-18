package yokai.data.collection

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.database.models.LibraryCollection
import kotlinx.coroutines.flow.Flow
import yokai.data.DatabaseHandler
import yokai.domain.collection.CollectionRepository
import yokai.domain.collection.models.CollectionUpdate

class CollectionRepositoryImpl(private val handler: DatabaseHandler) : CollectionRepository {

    override suspend fun getAll(): List<LibraryCollection> =
        handler.awaitList { collectionsQueries.findAll(LibraryCollection::mapper) }

    override fun getAllAsFlow(): Flow<List<LibraryCollection>> =
        handler.subscribeToList { collectionsQueries.findAll(LibraryCollection::mapper) }

    override suspend fun getById(id: Long): LibraryCollection? =
        handler.awaitOneOrNull { collectionsQueries.findById(id, LibraryCollection::mapper) }

    override suspend fun insert(collection: LibraryCollection): Long? =
        handler.awaitOneOrNullExecutable {
            collectionsQueries.insert(
                name = collection.name,
                sort = collection.order.toLong(),
            )
            collectionsQueries.selectLastInsertedRowId()
        }

    override suspend fun update(update: CollectionUpdate): Boolean {
        return try {
            handler.await(inTransaction = true) {
                collectionsQueries.update(
                    id = update.id,
                    name = update.name,
                    sort = update.order,
                )
            }
            true
        } catch (e: Exception) {
            Logger.e { "Failed to update collection with id '${update.id}'" }
            false
        }
    }

    override suspend fun delete(id: Long) {
        handler.await { collectionsQueries.delete(id) }
    }

    override suspend fun addManga(collectionId: Long, mangaId: Long) {
        handler.await { mangas_collectionsQueries.insert(mangaId, collectionId) }
    }

    override suspend fun removeManga(collectionId: Long, mangaId: Long) {
        handler.await { mangas_collectionsQueries.deleteByMangaAndCollection(mangaId, collectionId) }
    }

    override suspend fun getMangaIds(collectionId: Long): List<Long> =
        handler.awaitList { mangas_collectionsQueries.findMangaIdsByCollectionId(collectionId) }

    override fun getMangaIdsAsFlow(collectionId: Long): Flow<List<Long>> =
        handler.subscribeToList { mangas_collectionsQueries.findMangaIdsByCollectionId(collectionId) }

    override suspend fun getCollectionIdsByMangaId(mangaId: Long): List<Long> =
        handler.awaitList { mangas_collectionsQueries.findCollectionIdsByMangaId(mangaId) }

    override suspend fun getMangaCount(collectionId: Long): Long =
        handler.awaitOne { mangas_collectionsQueries.countByCollectionId(collectionId) }
}
