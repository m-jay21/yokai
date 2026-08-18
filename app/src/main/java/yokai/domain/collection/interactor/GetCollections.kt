package yokai.domain.collection.interactor

import yokai.domain.collection.CollectionRepository

class GetCollections(
    private val collectionRepository: CollectionRepository,
) {
    suspend fun await() = collectionRepository.getAll()
    suspend fun awaitById(id: Long) = collectionRepository.getById(id)
    fun subscribe() = collectionRepository.getAllAsFlow()
    suspend fun awaitMangaIds(collectionId: Long) = collectionRepository.getMangaIds(collectionId)
    fun subscribeMangaIds(collectionId: Long) = collectionRepository.getMangaIdsAsFlow(collectionId)
    suspend fun awaitCollectionIdsByMangaId(mangaId: Long) =
        collectionRepository.getCollectionIdsByMangaId(mangaId)
    suspend fun awaitMangaCount(collectionId: Long) = collectionRepository.getMangaCount(collectionId)
}
