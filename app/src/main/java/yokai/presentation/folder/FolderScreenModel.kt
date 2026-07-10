package yokai.presentation.folder

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.database.models.Folder
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.injectLazy
import yokai.domain.folder.interactor.DeleteFolder
import yokai.domain.folder.interactor.GetFolders
import yokai.domain.folder.interactor.InsertFolder
import yokai.domain.folder.interactor.SetFolderChapters
import yokai.domain.folder.interactor.UpdateFolder
import yokai.domain.folder.models.FolderUpdate

class FolderScreenModel : StateScreenModel<FolderScreenModel.State>(State.Loading) {

    private val getFolders: GetFolders by injectLazy()
    private val setFolderChapters: SetFolderChapters by injectLazy()
    private val insertFolder: InsertFolder by injectLazy()
    private val deleteFolder: DeleteFolder by injectLazy()
    private val updateFolder: UpdateFolder by injectLazy()

    init {
        screenModelScope.launchIO {
            getFolders.subscribeRoots().collectLatest { folders ->
                val items = folders.map { folder ->
                    FolderItem(
                        folder = folder,
                        chapterCount = setFolderChapters.getChapterIds(folder.id!!.toLong()).size,
                    )
                }
                mutableState.update { State.Success(items.toImmutableList()) }
            }
        }
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        screenModelScope.launchIO {
            val existing = getFolders.await()
            if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) return@launchIO
            val folder = Folder.create(trimmed).apply {
                order = (existing.maxOfOrNull { it.order } ?: 0) + 1
            }
            insertFolder.await(folder)
        }
    }

    fun deleteFolder(id: Long) {
        screenModelScope.launchIO {
            deleteFolder.await(id)
        }
    }

    fun renameFolder(id: Long, name: String) {
        screenModelScope.launchIO {
            updateFolder.await(FolderUpdate(id = id, name = name))
        }
    }

    @Immutable
    data class FolderItem(
        val folder: Folder,
        val chapterCount: Int,
    )

    sealed interface State {

        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val folders: ImmutableList<FolderItem>,
        ) : State {

            val isEmpty: Boolean
                get() = folders.isEmpty()
        }
    }
}
