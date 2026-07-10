package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupFolder
import eu.kanade.tachiyomi.data.backup.models.BackupFolderChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.folder.FolderRepository
import yokai.domain.folder.interactor.GetFolders

class FoldersBackupCreator(
    private val getFolders: GetFolders = Injekt.get(),
    private val folderRepository: FolderRepository = Injekt.get(),
) {
    suspend operator fun invoke(): List<BackupFolder> {
        val roots = getFolders.awaitRoots()
        return roots.map { folder -> buildBackupFolder(folder.id!!.toLong()) }
    }

    private suspend fun buildBackupFolder(folderId: Long): BackupFolder {
        val folder = getFolders.awaitById(folderId)
        val chapters = folderRepository.getChaptersWithManga(folderId).map { chapter ->
            BackupFolderChapter(
                mangaSource = chapter.mangaSource,
                mangaUrl = chapter.mangaUrl,
                chapterUrl = chapter.chapterUrl,
            )
        }
        val children = getFolders.awaitChildren(folderId)
        val subfolders = children.map { child ->
            buildBackupFolder(child.id!!.toLong())
        }
        return BackupFolder(
            name = folder?.name.orEmpty(),
            sort = folder?.order ?: 0,
            chapters = chapters,
            subfolders = subfolders,
            description = folder?.description,
            cover = folder?.cover,
            author = folder?.author,
            artist = folder?.artist,
            genre = folder?.genre,
        )
    }
}
