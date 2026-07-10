package yokai.domain.folder.interactor

import yokai.domain.folder.FolderRepository

class SetFolderChapters(
    private val folderRepository: FolderRepository,
) {
    suspend fun add(folderId: Long, chapterId: Long) =
        folderRepository.addChapter(folderId, chapterId)

    suspend fun remove(folderId: Long, chapterId: Long) =
        folderRepository.removeChapter(folderId, chapterId)

    suspend fun getChapterIds(folderId: Long) =
        folderRepository.getChapterIds(folderId)
}
