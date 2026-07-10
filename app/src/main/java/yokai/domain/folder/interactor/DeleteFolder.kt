package yokai.domain.folder.interactor

import yokai.domain.folder.FolderRepository

class DeleteFolder(
    private val folderRepository: FolderRepository,
) {
    suspend fun await(id: Long) = folderRepository.delete(id)
}
