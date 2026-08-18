package yokai.presentation.folder

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.database.models.LibraryCollection
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.injectLazy
import yokai.domain.collection.interactor.DeleteCollection
import yokai.domain.collection.interactor.GetCollections
import yokai.domain.collection.interactor.InsertCollection
import yokai.domain.collection.interactor.UpdateCollection
import yokai.domain.collection.models.CollectionUpdate

class CollectionScreenModel : StateScreenModel<CollectionScreenModel.State>(State.Loading) {

    private val getCollections: GetCollections by injectLazy()
    private val insertCollection: InsertCollection by injectLazy()
    private val deleteCollection: DeleteCollection by injectLazy()
    private val updateCollection: UpdateCollection by injectLazy()

    init {
        screenModelScope.launchIO {
            getCollections.subscribe().collectLatest { collections ->
                val items = collections.map { collection ->
                    CollectionItem(
                        collection = collection,
                        seriesCount = getCollections.awaitMangaCount(collection.id!!.toLong()).toInt(),
                    )
                }
                mutableState.update { State.Success(items.toImmutableList()) }
            }
        }
    }

    fun createCollection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        screenModelScope.launchIO {
            val existing = getCollections.await()
            if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) return@launchIO
            val collection = LibraryCollection.create(trimmed).apply {
                order = (existing.maxOfOrNull { it.order } ?: 0) + 1
            }
            insertCollection.await(collection)
        }
    }

    fun deleteCollection(id: Long) {
        screenModelScope.launchIO {
            deleteCollection.await(id)
        }
    }

    fun renameCollection(id: Long, name: String) {
        screenModelScope.launchIO {
            updateCollection.await(CollectionUpdate(id = id, name = name))
        }
    }

    @Immutable
    data class CollectionItem(
        val collection: LibraryCollection,
        val seriesCount: Int,
    )

    sealed interface State {

        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val collections: ImmutableList<CollectionItem>,
        ) : State {

            val isEmpty: Boolean
                get() = collections.isEmpty()
        }
    }
}
