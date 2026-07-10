package yokai.domain.folder.interactor

import eu.kanade.tachiyomi.data.database.models.Folder
import yokai.domain.folder.FolderRepository

class InsertFolder(
    private val folderRepository: FolderRepository,
) {
    suspend fun await(folder: Folder) = folderRepository.insert(folder)
}
