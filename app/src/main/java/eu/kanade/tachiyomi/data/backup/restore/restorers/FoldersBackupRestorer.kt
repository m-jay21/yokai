package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupFolder
import eu.kanade.tachiyomi.data.database.models.Folder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.data.DatabaseHandler
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.folder.FolderRepository
import yokai.domain.folder.interactor.GetFolders
import yokai.domain.folder.interactor.UpdateFolder
import yokai.domain.folder.models.FolderUpdate
import yokai.domain.manga.interactor.GetManga

class FoldersBackupRestorer(
    private val getFolders: GetFolders = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val folderRepository: FolderRepository = Injekt.get(),
    private val updateFolder: UpdateFolder = Injekt.get(),
    private val handler: DatabaseHandler = Injekt.get(),
) {
    suspend fun restoreFolders(backupFolders: List<BackupFolder>, onComplete: () -> Unit) {
        handler.await(true) {
            backupFolders.forEach { backupFolder ->
                restoreFolder(backupFolder, parentId = null)
            }
        }
        onComplete()
    }

    private suspend fun restoreFolder(backupFolder: BackupFolder, parentId: Long?) {
        val existingFolders = if (parentId == null) {
            getFolders.awaitRoots()
        } else {
            getFolders.awaitChildren(parentId)
        }
        val existingFolder = existingFolders.find { it.name == backupFolder.name }
        val folderId = if (existingFolder != null) {
            existingFolder.id!!.toLong()
        } else {
            val folder = Folder.create(backupFolder.name, parentId = parentId).apply {
                order = backupFolder.sort
                description = backupFolder.description
                cover = backupFolder.cover
                author = backupFolder.author
                artist = backupFolder.artist
                genre = backupFolder.genre
            }
            folderRepository.insert(folder) ?: return
        }

        // Restore chapters for this folder
        val existingChapterIds = folderRepository.getChapterIds(folderId).toSet()
        val chapterIds = mutableListOf<Long>()

        backupFolder.chapters.forEach { ref ->
            val manga = getManga.awaitByUrlAndSource(ref.mangaUrl, ref.mangaSource) ?: return@forEach
            val filterScanlators = manga.filtered_scanlators?.isNotEmpty() == true
            val chapter = getChapter.awaitByUrlAndMangaId(
                ref.chapterUrl,
                manga.id!!,
                filterScanlators,
            ) ?: return@forEach

            if (chapter.id !in existingChapterIds) {
                folderRepository.addChapter(folderId, chapter.id!!)
            }
            chapterIds.add(chapter.id!!)
        }

        if (chapterIds.isNotEmpty()) {
            updateFolder.await(
                FolderUpdate(
                    id = folderId,
                    chapterOrder = chapterIds.joinToString("/"),
                ),
            )
        }

        // Recursively restore subfolders
        backupFolder.subfolders.forEach { sub ->
            restoreFolder(sub, parentId = folderId)
        }
    }
}
