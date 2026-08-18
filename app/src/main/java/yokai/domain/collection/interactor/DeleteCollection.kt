package yokai.domain.collection.interactor

import yokai.domain.collection.CollectionRepository

class DeleteCollection(
    private val collectionRepository: CollectionRepository,
) {
    suspend fun await(id: Long) = collectionRepository.delete(id)
}
