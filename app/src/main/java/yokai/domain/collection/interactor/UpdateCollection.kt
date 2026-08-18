package yokai.domain.collection.interactor

import yokai.domain.collection.CollectionRepository
import yokai.domain.collection.models.CollectionUpdate

class UpdateCollection(
    private val collectionRepository: CollectionRepository,
) {
    suspend fun await(update: CollectionUpdate) = collectionRepository.update(update)
}
