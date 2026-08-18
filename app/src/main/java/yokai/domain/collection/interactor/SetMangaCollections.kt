package yokai.domain.collection.interactor

import yokai.domain.collection.CollectionRepository

class SetMangaCollections(
    private val collectionRepository: CollectionRepository,
) {
    suspend fun add(collectionId: Long, mangaId: Long) =
        collectionRepository.addManga(collectionId, mangaId)

    suspend fun remove(collectionId: Long, mangaId: Long) =
        collectionRepository.removeManga(collectionId, mangaId)

    suspend fun getMangaIds(collectionId: Long) =
        collectionRepository.getMangaIds(collectionId)
}
