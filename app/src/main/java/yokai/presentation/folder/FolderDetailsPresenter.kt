package yokai.presentation.folder

import android.app.Application
import android.net.Uri
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Folder
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.manga.MangaDetailsData
import eu.kanade.tachiyomi.ui.manga.MangaHeaderItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.chapter.interactor.UpdateChapter
import yokai.domain.chapter.models.ChapterUpdate
import yokai.domain.folder.interactor.GetFolders
import yokai.domain.folder.interactor.SetFolderChapters
import yokai.domain.folder.interactor.UpdateFolder
import yokai.domain.folder.models.FolderUpdate
import yokai.domain.manga.interactor.GetManga
import yokai.i18n.MR
import yokai.util.lang.getString
import java.io.File
import java.io.FileOutputStream

class FolderDetailsPresenter(
    val folderId: Long,
    override val preferences: PreferencesHelper = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
) : BaseCoroutinePresenter<FolderDetailsController>(),
    MangaDetailsData {

    private val getFolders: GetFolders by injectLazy()
    private val updateFolder: UpdateFolder by injectLazy()
    private val setFolderChapters: SetFolderChapters by injectLazy()
    private val getManga: GetManga by injectLazy()
    private val getChapter: GetChapter by injectLazy()
    private val updateChapter: UpdateChapter by injectLazy()

    lateinit var folder: Folder
        private set

    fun isFolderLoaded(): Boolean = ::folder.isInitialized

    private var _manga: Manga = createSyntheticManga(null, null)
    override val manga: Manga get() = _manga

    override var chapters: List<ChapterItem> = emptyList()
        private set

    var allChapters: List<ChapterItem> = emptyList()
        private set

    override val source: Source by lazy { sourceManager.getOrStub(-1) }

    val headerItem: MangaHeaderItem by lazy { MangaHeaderItem(folderId, false) }
    var tabletChapterHeaderItem: MangaHeaderItem? = null

    var scrollType = 0
    var isLoading = false

    private var readFilter = TriState.IGNORE
    private var downloadedFilter = TriState.IGNORE
    private var bookmarkedFilter = TriState.IGNORE
    var freeMovementEnabled = true
        private set

    enum class TriState { IGNORE, INCLUDE, EXCLUDE }

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            load()
            merge(
                downloadManager.queueState.drop(1),
                downloadManager.statusFlow(),
                downloadManager.progressFlow(),
            ).collect {
                refreshDownloadStates()
                withUIContext { view?.updateChapters() }
            }
        }
    }

    fun load() {
        presenterScope.launchIO {
            isLoading = true
            withUIContext { view?.setRefreshing(true) }
            try {
                folder = getFolders.awaitById(folderId) ?: return@launchIO
                val folderChapters = getFolders.awaitFolderChapters(folderId)
                allChapters = folderChapters.mapNotNull { fc ->
                    val chapter = getChapter.awaitById(fc.chapterId) ?: return@mapNotNull null
                    val manga = getManga.awaitById(fc.mangaId) ?: return@mapNotNull null
                    chapter.toItem(manga)
                }.let { items ->
                    applyChapterOrder(items, folder.chapterOrder)
                }
                val coverFallback = allChapters.firstOrNull()?.manga?.thumbnail_url
                _manga = createSyntheticManga(folder, coverFallback)
                applyFilters()
            } catch (e: Exception) {
                Logger.e(e) { "Failed loading folder $folderId" }
            } finally {
                isLoading = false
                withUIContext {
                    view?.setRefreshing(false)
                    view?.updateHeader()
                    view?.updateChapters()
                }
            }
        }
    }

    private fun applyChapterOrder(items: List<ChapterItem>, order: List<Long>): List<ChapterItem> {
        if (order.isEmpty()) return items
        val byId = items.associateBy { it.chapter.id }
        val ordered = order.mapNotNull { byId[it] }
        val remaining = items.filter { it.chapter.id !in order }
        return ordered + remaining
    }

    private fun Chapter.toItem(manga: Manga): ChapterItem {
        val model = ChapterItem(this, manga)
        val download = downloadManager.queueState.value.find { it.chapter.id == id }
        if (download != null) {
            model.status = download.status
            model.download = download
        } else if (downloadManager.isChapterDownloaded(this, manga)) {
            model.status = Download.State.DOWNLOADED
        }
        return model
    }

    private fun applyFilters() {
        chapters = allChapters.filter { item ->
            when (readFilter) {
                TriState.INCLUDE -> if (!item.read) return@filter false
                TriState.EXCLUDE -> if (item.read) return@filter false
                TriState.IGNORE -> {}
            }
            val downloaded = item.status == Download.State.DOWNLOADED
            when (downloadedFilter) {
                TriState.INCLUDE -> if (!downloaded) return@filter false
                TriState.EXCLUDE -> if (downloaded) return@filter false
                TriState.IGNORE -> {}
            }
            when (bookmarkedFilter) {
                TriState.INCLUDE -> if (!item.bookmark) return@filter false
                TriState.EXCLUDE -> if (item.bookmark) return@filter false
                TriState.IGNORE -> {}
            }
            true
        }
    }

    private fun refreshDownloadStates() {
        allChapters.forEach { item ->
            val manga = item.manga
            val chapter = item.chapter
            val queued = downloadManager.getChapterDownloadOrNull(chapter)
            if (queued != null) {
                item.status = queued.status
                item.download = queued
            } else if (downloadManager.isChapterDownloaded(chapter, manga)) {
                item.status = Download.State.DOWNLOADED
                item.download = null
            } else {
                item.status = Download.State.NOT_DOWNLOADED
                item.download = null
            }
        }
        applyFilters()
    }

    override fun currentFilters(): String {
        val context = view?.view?.context ?: return ""
        return buildList {
            if (readFilter == TriState.INCLUDE) add(MR.strings.read)
            if (readFilter == TriState.EXCLUDE) add(MR.strings.unread)
            if (downloadedFilter == TriState.INCLUDE) add(MR.strings.downloaded)
            if (downloadedFilter == TriState.EXCLUDE) add(MR.strings.not_downloaded)
            if (bookmarkedFilter == TriState.INCLUDE) add(MR.strings.bookmarked)
            if (bookmarkedFilter == TriState.EXCLUDE) add(MR.strings.not_bookmarked)
            if (freeMovementEnabled) add(MR.strings.free_movement)
        }.joinToString(", ") { context.getString(it) }
    }

    fun toggleFreeMovement() {
        freeMovementEnabled = !freeMovementEnabled
        view?.updateChapters()
    }

    fun setReadFilter(state: TriState) {
        readFilter = state
        applyFilters()
        view?.updateChapters()
    }

    fun setDownloadedFilter(state: TriState) {
        downloadedFilter = state
        applyFilters()
        view?.updateChapters()
    }

    fun setBookmarkedFilter(state: TriState) {
        bookmarkedFilter = state
        applyFilters()
        view?.updateChapters()
    }

    fun cycleReadFilter() = setReadFilter(readFilter.next())
    fun cycleDownloadedFilter() = setDownloadedFilter(downloadedFilter.next())
    fun cycleBookmarkedFilter() = setBookmarkedFilter(bookmarkedFilter.next())
    fun readFilterState() = readFilter
    fun downloadedFilterState() = downloadedFilter
    fun bookmarkedFilterState() = bookmarkedFilter

    private fun TriState.next() = when (this) {
        TriState.IGNORE -> TriState.INCLUDE
        TriState.INCLUDE -> TriState.EXCLUDE
        TriState.EXCLUDE -> TriState.IGNORE
    }

    override fun getNextUnreadChapter(): ChapterItem? = chapters.firstOrNull { !it.read }

    override fun isTracked(): Boolean = false
    override fun hasTrackers(): Boolean = false

    fun anyRead(): Boolean = allChapters.any { it.read }
    fun hasBookmark(): Boolean = allChapters.any { it.bookmark }
    fun hasDownloads(): Boolean = allChapters.any { it.status == Download.State.DOWNLOADED }

    fun downloadChapter(item: ChapterItem) {
        presenterScope.launchIO {
            val source = sourceManager.getOrStub(item.manga.source)
            if (downloadManager.isChapterDownloaded(item.chapter, item.manga) ||
                item.status == Download.State.DOWNLOADED ||
                item.status == Download.State.QUEUE ||
                item.status == Download.State.DOWNLOADING
            ) {
                downloadManager.deleteChapters(listOf(item.chapter), item.manga, source, true)
            } else {
                downloadManager.downloadChapters(item.manga, listOf(item.chapter))
            }
            refreshDownloadStates()
            withUIContext { view?.updateChapters() }
        }
    }

    fun startDownloadNow(item: ChapterItem) {
        downloadManager.startDownloadNow(item.chapter)
    }

    fun downloadChapters(items: List<ChapterItem>) {
        items.groupBy { it.manga.id }.forEach { (_, group) ->
            val manga = group.first().manga
            downloadManager.downloadChapters(manga, group.map { it.chapter })
        }
    }

    fun deleteChapters(items: List<ChapterItem>) {
        presenterScope.launchIO {
            items.groupBy { it.manga }.forEach { (manga, group) ->
                val source = sourceManager.getOrStub(manga.source)
                downloadManager.deleteChapters(group.map { it.chapter }, manga, source, true)
            }
            refreshDownloadStates()
            withUIContext { view?.updateChapters() }
        }
    }

    fun reorderChapters(visibleOrder: List<Long>) {
        presenterScope.launchIO {
            val visibleSet = visibleOrder.toSet()
            var visibleIndex = 0
            val merged = allChapters.mapNotNull { it.chapter.id }.map { id ->
                if (id in visibleSet) visibleOrder[visibleIndex++] else id
            }
            updateFolder.await(
                FolderUpdate(id = folderId, chapterOrder = merged.joinToString("/")),
            )
            load()
        }
    }

    fun removeChapterFromFolder(item: ChapterItem) {
        presenterScope.launchIO {
            val chapterId = item.chapter.id ?: return@launchIO
            setFolderChapters.remove(folderId, chapterId)
            load()
        }
    }

    fun markChaptersRead(items: List<ChapterItem>, read: Boolean) {
        presenterScope.launchIO {
            updateChapter.awaitAll(
                items.mapNotNull { item ->
                    val id = item.chapter.id ?: return@mapNotNull null
                    ChapterUpdate(
                        id = id,
                        read = read,
                        lastPageRead = if (read) null else 0L,
                    )
                },
            )
            load()
        }
    }

    fun bookmarkChapters(items: List<ChapterItem>, bookmarked: Boolean) {
        presenterScope.launchIO {
            updateChapter.awaitAll(
                items.mapNotNull { item ->
                    val id = item.chapter.id ?: return@mapNotNull null
                    ChapterUpdate(id = id, bookmark = bookmarked)
                },
            )
            load()
        }
    }

    fun updateMetadata(
        name: String?,
        description: String?,
        author: String?,
        artist: String?,
        genre: String?,
        coverUri: Uri?,
        resetCover: Boolean,
    ) {
        presenterScope.launchIO {
            val coverPath = when {
                resetCover -> ""
                coverUri != null -> persistCover(coverUri)
                else -> null
            }
            updateFolder.await(
                FolderUpdate(
                    id = folderId,
                    name = name?.takeIf { it.isNotBlank() },
                    description = description,
                    author = author,
                    artist = artist,
                    genre = genre,
                    cover = coverPath,
                ),
            )
            load()
        }
    }

    private fun persistCover(uri: Uri): String? {
        return try {
            val context = Injekt.get<Application>()
            val dir = File(context.filesDir, "folder_covers").apply { mkdirs() }
            val dest = File(dir, "folder_$folderId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) {
            Logger.e(e) { "Failed saving folder cover" }
            null
        }
    }

    companion object {
        fun createSyntheticManga(folder: Folder?, coverFallback: String?): Manga {
            return MangaImpl(
                id = -(folder?.id?.toLong() ?: 1L).coerceAtLeast(1L),
                source = -1L,
                url = "folder://${folder?.id ?: 0}",
            ).also {
                it.ogTitle = folder?.name ?: ""
                it.author = folder?.author
                it.artist = folder?.artist
                it.description = folder?.description
                it.genre = folder?.genre
                it.thumbnail_url = folder?.cover?.takeIf { c -> c.isNotBlank() } ?: coverFallback
                it.favorite = false
                it.initialized = true
                it.status = SManga.UNKNOWN
            }
        }
    }
}
