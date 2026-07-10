package eu.kanade.tachiyomi.ui.manga

import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.databinding.MangaDetailsControllerBinding

/**
 * Shared host so [MangaDetailsAdapter]/header/chapter holders can drive
 * either manga details or folder details Controllers.
 */
interface MangaDetailsHost : MangaDetailsAdapter.MangaDetailsInterface {
    val detailsData: MangaDetailsData
    val binding: MangaDetailsControllerBinding
    val scrollType: Int
    val recyclerView: RecyclerView
        get() = binding.recycler

    /** Whether the chapter list supports manual drag-to-reorder (folders only). */
    val allowChapterReorder: Boolean
        get() = false

    /** When [allowChapterReorder] is true, whether free-movement mode is currently enabled. */
    val chapterReorderEnabled: Boolean
        get() = allowChapterReorder

    /** Folder details uses the same header layout but needs different chrome. */
    val isFolderDetails: Boolean
        get() = false

    fun bookmarkChapter(position: Int)
    fun toggleReadChapter(position: Int)
    fun dismissPopup(position: Int)

    /** Called once a chapter drag finishes with the chapter ids in their new visible order. */
    fun reorderChapters(chapterIds: List<Long>) {}
}
