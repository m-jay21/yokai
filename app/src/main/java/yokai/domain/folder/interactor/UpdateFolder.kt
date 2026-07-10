package yokai.domain.folder.interactor

import yokai.domain.folder.FolderRepository
import yokai.domain.folder.models.FolderUpdate

class UpdateFolder(
    private val folderRepository: FolderRepository,
) {
    suspend fun await(update: FolderUpdate) = folderRepository.update(update)
}
