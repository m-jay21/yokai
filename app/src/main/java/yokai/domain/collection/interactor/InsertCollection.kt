package yokai.domain.collection.interactor

import eu.kanade.tachiyomi.data.database.models.LibraryCollection
import yokai.domain.collection.CollectionRepository

class InsertCollection(
    private val collectionRepository: CollectionRepository,
) {
    suspend fun await(collection: LibraryCollection) = collectionRepository.insert(collection)
}
