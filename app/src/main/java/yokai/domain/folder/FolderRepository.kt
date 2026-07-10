package yokai.domain.folder

import eu.kanade.tachiyomi.data.database.models.Folder
import kotlinx.coroutines.flow.Flow
import yokai.domain.folder.models.FolderUpdate

interface FolderRepository {
    suspend fun getAll(): List<Folder>
    suspend fun getRoots(): List<Folder>
    suspend fun getChildren(parentId: Long): List<Folder>
    fun getRootsAsFlow(): Flow<List<Folder>>
    suspend fun getById(id: Long): Folder?
    suspend fun getAllByChapterId(chapterId: Long): List<Folder>
    fun getAllAsFlow(): Flow<List<Folder>>
    suspend fun insert(folder: Folder): Long?
    suspend fun update(update: FolderUpdate): Boolean
    suspend fun delete(id: Long)
    suspend fun addChapter(folderId: Long, chapterId: Long)
    suspend fun removeChapter(folderId: Long, chapterId: Long)
    suspend fun getChapterIds(folderId: Long): List<Long>
    suspend fun getChaptersWithManga(folderId: Long): List<FolderChapter>
}

data class FolderChapter(
    val mangaId: Long,
    val mangaTitle: String,
    val mangaUrl: String,
    val mangaThumbnailUrl: String?,
    val mangaSource: Long,
    val chapterId: Long,
    val chapterName: String,
    val chapterNumber: Double,
    val chapterRead: Boolean,
    val chapterBookmark: Boolean,
    val chapterMangaId: Long,
    val chapterUrl: String,
    val scanlator: String?,
    val lastPageRead: Int,
    val dateUpload: Long,
    val dateFetch: Long,
    val isDownloaded: Boolean = false,
)
