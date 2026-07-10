package yokai.domain.folder.interactor

import yokai.domain.folder.FolderRepository

class GetFolders(
    private val folderRepository: FolderRepository,
) {
    suspend fun await() = folderRepository.getAll()
    suspend fun awaitRoots() = folderRepository.getRoots()
    suspend fun awaitChildren(parentId: Long) = folderRepository.getChildren(parentId)
    suspend fun awaitById(id: Long) = folderRepository.getById(id)
    suspend fun awaitByChapterId(chapterId: Long) = folderRepository.getAllByChapterId(chapterId)
    fun subscribe() = folderRepository.getAllAsFlow()
    fun subscribeRoots() = folderRepository.getRootsAsFlow()
    suspend fun awaitFolderChapters(folderId: Long) = folderRepository.getChaptersWithManga(folderId)
}
