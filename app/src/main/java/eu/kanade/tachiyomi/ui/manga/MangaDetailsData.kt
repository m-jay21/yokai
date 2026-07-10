package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem

/**
 * Shared surface used by manga/folder details holders so both screens can
 * reuse the same header/chapter layouts.
 */
interface MangaDetailsData {
    val manga: Manga
    val chapters: List<ChapterItem>
    val preferences: PreferencesHelper
    val source: Source
    fun currentFilters(): String
    fun getNextUnreadChapter(): ChapterItem?
    fun isTracked(): Boolean
    fun hasTrackers(): Boolean
}
