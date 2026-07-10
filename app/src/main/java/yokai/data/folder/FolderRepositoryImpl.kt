package yokai.data.folder

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.database.models.Folder
import kotlinx.coroutines.flow.Flow
import yokai.data.DatabaseHandler
import yokai.domain.folder.FolderChapter
import yokai.domain.folder.FolderRepository
import yokai.domain.folder.models.FolderUpdate

class FolderRepositoryImpl(private val handler: DatabaseHandler) : FolderRepository {

    override suspend fun getAll(): List<Folder> =
        handler.awaitList { foldersQueries.findAll(Folder::mapper) }

    override suspend fun getRoots(): List<Folder> =
        handler.awaitList { foldersQueries.findRoots(Folder::mapper) }

    override suspend fun getChildren(parentId: Long): List<Folder> =
        handler.awaitList { foldersQueries.findByParentId(parentId, Folder::mapper) }

    override fun getRootsAsFlow(): Flow<List<Folder>> =
        handler.subscribeToList { foldersQueries.findRoots(Folder::mapper) }

    override suspend fun getById(id: Long): Folder? =
        handler.awaitOneOrNull { foldersQueries.findById(id, Folder::mapper) }

    override suspend fun getAllByChapterId(chapterId: Long): List<Folder> =
        handler.awaitList { foldersQueries.findAllByChapterId(chapterId, Folder::mapper) }

    override fun getAllAsFlow(): Flow<List<Folder>> =
        handler.subscribeToList { foldersQueries.findAll(Folder::mapper) }

    override suspend fun insert(folder: Folder): Long? =
        handler.awaitOneOrNullExecutable {
            foldersQueries.insert(
                name = folder.name,
                sort = folder.order.toLong(),
                chapterOrder = folder.chapterOrderToString(),
                parentId = folder.parentId,
                description = folder.description,
                cover = folder.cover,
                author = folder.author,
                artist = folder.artist,
                genre = folder.genre,
            )
            foldersQueries.selectLastInsertedRowId()
        }

    override suspend fun update(update: FolderUpdate): Boolean {
        return try {
            handler.await(inTransaction = true) {
                foldersQueries.update(
                    id = update.id,
                    name = update.name,
                    chapterOrder = update.chapterOrder,
                    sort = update.order,
                    parentId = update.parentId,
                    clearParent = if (update.clearParent) 1L else 0L,
                    description = update.description,
                    cover = update.cover,
                    author = update.author,
                    artist = update.artist,
                    genre = update.genre,
                )
            }
            true
        } catch (e: Exception) {
            Logger.e { "Failed to update folder with id '${update.id}'" }
            false
        }
    }

    override suspend fun delete(id: Long) {
        handler.await { foldersQueries.delete(id) }
    }

    override suspend fun addChapter(folderId: Long, chapterId: Long) {
        handler.await(inTransaction = true) {
            folder_chaptersQueries.insert(folderId, chapterId)
        }
        val folder = getById(folderId) ?: return
        if (chapterId in folder.chapterOrder) return
        update(
            FolderUpdate(
                id = folderId,
                chapterOrder = (folder.chapterOrder + chapterId).joinToString("/"),
            ),
        )
    }

    override suspend fun removeChapter(folderId: Long, chapterId: Long) {
        handler.await {
            folder_chaptersQueries.deleteByFolderAndChapter(folderId, chapterId)
        }
        val folder = getById(folderId) ?: return
        update(
            FolderUpdate(
                id = folderId,
                chapterOrder = folder.chapterOrder.filter { it != chapterId }.joinToString("/"),
            ),
        )
    }

    override suspend fun getChapterIds(folderId: Long): List<Long> =
        handler.awaitList { folder_chaptersQueries.findChapterIdsByFolderId(folderId) }

    override suspend fun getChaptersWithManga(folderId: Long): List<FolderChapter> {
        val folder = getById(folderId)
        val chapters = handler.awaitList {
            folder_chaptersQueries.findChaptersWithMangaByFolderId(folderId) { mangaId, source, mangaUrl, _, _, _, _, title, _, thumbnailUrl, _, _, _, _, _, _, _, _, _, _, chapterId, chapterMangaId, chapterUrl, name, scanlator, read, bookmark, lastPageRead, _, chapterNumber, _, dateFetch, dateUpload ->
                FolderChapter(
                    mangaId = mangaId,
                    mangaTitle = title,
                    mangaUrl = mangaUrl,
                    mangaThumbnailUrl = thumbnailUrl,
                    mangaSource = source,
                    chapterId = chapterId!!,
                    chapterName = name!!,
                    chapterNumber = chapterNumber!!,
                    chapterRead = read!!,
                    chapterBookmark = bookmark!!,
                    chapterMangaId = chapterMangaId!!,
                    chapterUrl = chapterUrl!!,
                    scanlator = scanlator,
                    lastPageRead = lastPageRead!!.toInt(),
                    dateUpload = dateUpload!!,
                    dateFetch = dateFetch!!,
                )
            }
        }
        return sortChaptersByOrder(chapters, folder?.chapterOrder.orEmpty())
    }

    private fun sortChaptersByOrder(
        chapters: List<FolderChapter>,
        order: List<Long>,
    ): List<FolderChapter> {
        if (order.isEmpty()) return chapters
        val chapterMap = chapters.associateBy { it.chapterId }
        val ordered = order.mapNotNull { chapterMap[it] }
        val remaining = chapters.filter { it.chapterId !in order.toSet() }
        return ordered + remaining
    }
}
