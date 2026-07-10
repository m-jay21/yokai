package yokai.presentation.folder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.SizeResolver
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.snackbar.Snackbar
import dev.icerock.moko.resources.StringResource
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.coil.getBestColor
import eu.kanade.tachiyomi.data.database.models.vibrantCoverColor
import eu.kanade.tachiyomi.databinding.MangaDetailsControllerBinding
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsAdapter
import eu.kanade.tachiyomi.ui.manga.MangaDetailsData
import eu.kanade.tachiyomi.ui.manga.MangaDetailsDivider
import eu.kanade.tachiyomi.ui.manga.MangaDetailsHost
import eu.kanade.tachiyomi.ui.manga.MangaHeaderHolder
import eu.kanade.tachiyomi.ui.manga.MangaHeaderItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterHolder
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.isLandscape
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setMessage
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.setTextColorAlpha
import eu.kanade.tachiyomi.util.view.setTitle
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.toolbarHeight
import eu.kanade.tachiyomi.widget.LinearLayoutManagerAccurateOffset
import yokai.domain.manga.models.cover
import yokai.i18n.MR
import yokai.util.lang.getString
import kotlin.math.max
import kotlin.math.roundToInt
import android.R as AR

/**
 * Folder details – reuses series details layouts, holders, search, and download button.
 */
class FolderDetailsController :
    BaseCoroutineController<MangaDetailsControllerBinding, FolderDetailsPresenter>,
    MangaDetailsHost,
    SmallToolbarInterface,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemMoveListener {

    constructor(folderId: Long) : super(
        Bundle().apply { putLong(FOLDER_EXTRA, folderId) },
    ) {
        presenter = FolderDetailsPresenter(folderId)
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(FOLDER_EXTRA))

    override lateinit var presenter: FolderDetailsPresenter
        private set

    override val detailsData: MangaDetailsData get() = presenter
    override val scrollType: Int get() = presenter.scrollType
    override val allowChapterReorder: Boolean get() = true
    override val chapterReorderEnabled: Boolean get() = presenter.freeMovementEnabled
    override val isFolderDetails: Boolean get() = true

    private var adapter: MangaDetailsAdapter? = null
    private var tabletAdapter: MangaDetailsAdapter? = null
    private var isTablet = false
    private var query = ""
    private var headerHeight = 0
    private var toolbarIsColored = false
    private var coverColor: Int? = null
    private var accentColor: Int? = null
    private var headerColor: Int? = null
    private var editDialog: EditFolderDialog? = null
    private var chapterPopupMenu: PopupMenu? = null
    private var snack: Snackbar? = null
    private var pendingCoverUri: Uri? = null

    override fun getTitle(): String? =
        if (::presenter.isInitialized && presenter.isFolderLoaded()) presenter.folder.name else null

    override fun createBinding(inflater: LayoutInflater) =
        MangaDetailsControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        coverColor = null
        setAccentColorValue()
        setHeaderColorValue()
        setTabletMode(view)
        setRecycler(view)
        adapter?.fastScroller = binding.fastScroller
        binding.fab.setOnClickListener { readNextChapter(it) }
        binding.swipeRefresh.isEnabled = true
        binding.swipeRefresh.setOnRefreshListener { presenter.load() }
        setHasOptionsMenu(true)
        binding.fab.isVisible = false
        setPaletteColor()
        updateToolbarTitleAlpha()
        if (presenter.preferences.themeMangaDetails().get()) {
            setItemColors()
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter && isControllerVisible) {
            updateToolbarTitleAlpha(0f)
            colorToolbar(binding.recycler.canScrollVertically(-1))
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (presenter.isFolderLoaded()) {
            setPaletteColor()
        }
    }

    private fun setTabletMode(view: View) {
        isTablet = view.context.isTablet() && view.context.isLandscape()
        binding.tabletOverlay.isVisible = isTablet
        binding.tabletRecycler.isVisible = isTablet
        binding.tabletDivider.isVisible = isTablet
        if (isTablet) {
            // Match series details: shrink the chapter list so the left metadata
            // column (tablet_recycler) can claim ~40% of the width.
            binding.tabletRecycler.itemAnimator = null
            binding.recycler.updateLayoutParams<ViewGroup.LayoutParams> { width = 0 }
            binding.tabletRecycler.updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintPercentWidth = 0.4f
            }
            tabletAdapter = MangaDetailsAdapter(this)
            binding.tabletRecycler.adapter = tabletAdapter
            binding.tabletRecycler.layoutManager = LinearLayoutManager(view.context)
            presenter.tabletChapterHeaderItem = MangaHeaderItem(presenter.folderId, false).also {
                it.isChapterHeader = true
                it.isTablet = true
            }
            presenter.headerItem.isTablet = true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setRecycler(view: View) {
        adapter = MangaDetailsAdapter(this)
        binding.recycler.adapter = adapter
        adapter?.isSwipeEnabled = true
        adapter?.isHandleDragEnabled = true
        binding.recycler.layoutManager = LinearLayoutManagerAccurateOffset(view.context)
        binding.recycler.addItemDecoration(MangaDetailsDivider(view.context))
        binding.recycler.setHasFixedSize(true)
        val appbarHeight = activityBinding?.appBar?.attrToolbarHeight ?: 0
        val offset = 10.dpToPx
        binding.swipeRefresh.setDistanceToTriggerSync(70.dpToPx)
        if (isTablet) {
            val tHeight = toolbarHeight.takeIf { (it ?: 0) > 0 } ?: appbarHeight
            val insetsCompat = view.rootWindowInsetsCompat ?: activityBinding?.root?.rootWindowInsetsCompat
            headerHeight = tHeight + (insetsCompat?.getInsets(systemBars())?.top ?: 0)
            binding.recycler.updatePaddingRelative(top = headerHeight + 4.dpToPx)
        }
        scrollViewWith(
            binding.recycler,
            padBottom = true,
            customPadding = true,
            swipeRefreshLayout = binding.swipeRefresh,
            afterInsets = { insets -> setInsets(insets, appbarHeight, offset) },
            liftOnScroll = { colorToolbar(it) },
        )
        binding.recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (isTablet) return
                    colorToolbar(recyclerView.canScrollVertically(-1))
                    updateToolbarTitleAlpha(isScrollingDown = dy > 0)
                    val atTop = !recyclerView.canScrollVertically(-1)
                    val tY = getHeader()?.binding?.backdrop?.translationY ?: 0f
                    getHeader()?.binding?.backdrop?.translationY = max(0f, tY + dy * 0.25f)
                    if (atTop) getHeader()?.binding?.backdrop?.translationY = 0f
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    updateToolbarTitleAlpha()
                    if (!recyclerView.canScrollVertically(-1)) {
                        getHeader()?.binding?.backdrop?.translationY = 0f
                    }
                }
            },
        )
    }

    private fun setInsets(insets: WindowInsetsCompat, appbarHeight: Int, offset: Int) {
        val systemInsets = insets.getInsets(systemBars())
        binding.recycler.updatePaddingRelative(bottom = systemInsets.bottom)
        binding.tabletRecycler.updatePaddingRelative(bottom = systemInsets.bottom)
        val tHeight = toolbarHeight.takeIf { (it ?: 0) > 0 } ?: appbarHeight
        headerHeight = tHeight + systemInsets.top
        binding.swipeRefresh.setProgressViewOffset(false, (-40).dpToPx, headerHeight + offset)
        if (isTablet) {
            binding.tabletOverlay.updateLayoutParams<ViewGroup.LayoutParams> {
                height = headerHeight
            }
            binding.recycler.updatePaddingRelative(top = headerHeight + 4.dpToPx)
        }
        getHeader()?.setTopHeight(headerHeight)
        binding.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = headerHeight
            bottomMargin = systemInsets.bottom
        }
        binding.fastScroller.scrollOffset = headerHeight
    }

    private fun colorToolbar(isColor: Boolean) {
        if (isColor == toolbarIsColored || (isTablet && isColor)) return
        val activity = activity ?: return
        toolbarIsColored = isColor
        if (isControllerVisible) setTitle()
        updateToolbarTitleAlpha()
        val scrollingColor = headerColor ?: activity.getResourceColor(R.attr.colorPrimaryVariant)
        val topColor = ColorUtils.setAlphaComponent(scrollingColor, 0)
        val scrollingStatusColor =
            ColorUtils.setAlphaComponent(scrollingColor, (0.87f * 255).roundToInt())
        activityBinding?.appBar?.setBackgroundColor(if (toolbarIsColored) scrollingColor else topColor)
        activity.window?.statusBarColor =
            if (toolbarIsColored) scrollingStatusColor else Color.TRANSPARENT
    }

    private fun getHeader(): MangaHeaderHolder? =
        if (isTablet) {
            binding.tabletRecycler.findViewHolderForAdapterPosition(0) as? MangaHeaderHolder
        } else {
            binding.recycler.findViewHolderForAdapterPosition(0) as? MangaHeaderHolder
        }

    fun setRefreshing(refreshing: Boolean) {
        binding.swipeRefresh.isRefreshing = refreshing
    }

    fun updateHeader() {
        if (isControllerVisible) setTitle()
        updateToolbarTitleAlpha()
        addFolderHeader()
        tabletAdapter?.notifyItemChanged(0)
        setPaletteColor()
        if (presenter.preferences.themeMangaDetails().get()) {
            setItemColors()
        }
        updateMenuVisibility(activityBinding?.toolbar?.menu)
    }

    fun updateChapters() {
        adapter?.setChapters(presenter.chapters)
        addFolderHeader()
        tabletAdapter?.notifyItemChanged(0)
        binding.fab.isVisible = false
        colorToolbar(binding.recycler.canScrollVertically(-1))
        updateToolbarTitleAlpha()
        updateMenuVisibility(activityBinding?.toolbar?.menu)
    }

    private fun updateToolbarTitleAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float? = null, isScrollingDown: Boolean = false) {
        if ((!isControllerVisible && alpha == null) || isScrollingDown) return
        val scrolledList = binding.recycler
        val toolbarTextView = activityBinding?.toolbar?.toolbarTitle ?: return
        val tbAlpha = when {
            isTablet -> 0f
            alpha != null -> alpha
            ((scrolledList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0) -> 1f
            ((scrolledList.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) -> 0f
            else -> (scrolledList.computeVerticalScrollOffset() - (20.dpToPx))
                .coerceIn(0, 255) / 255f
        }
        toolbarTextView.setTextColorAlpha((tbAlpha * 255).roundToInt())
    }

    private fun setAccentColorValue(colorToUse: Int? = null) {
        val context = view?.context ?: return
        val manga = presenter.manga
        setCoverColorValue(colorToUse)
        accentColor = if (presenter.preferences.themeMangaDetails().get()) {
            (colorToUse ?: manga.vibrantCoverColor)?.let {
                val luminance = ColorUtils.calculateLuminance(it).toFloat()
                if (if (!context.isInNightMode()) luminance > 0.4 else luminance <= 0.6) {
                    ColorUtils.blendARGB(
                        it,
                        context.contextCompatColor(R.color.colorOnDownloadBadgeDayNight),
                        (if (!context.isInNightMode()) luminance else -(luminance - 1))
                            .toFloat() * if (context.isInNightMode()) 0.33f else 0.5f,
                    )
                } else {
                    it
                }
            }
        } else {
            null
        }
    }

    private fun setCoverColorValue(colorToUse: Int? = null) {
        val context = view?.context ?: return
        val manga = presenter.manga
        val colorBack = context.getResourceColor(R.attr.background)
        coverColor =
            (
                if (presenter.preferences.themeMangaDetails().get()) {
                    (colorToUse ?: manga.vibrantCoverColor)
                } else {
                    ColorUtils.blendARGB(
                        context.getResourceColor(R.attr.colorSecondary),
                        colorBack,
                        0.5f,
                    )
                }
                )?.let {
                val domLum = ColorUtils.calculateLuminance(it)
                val lumWrongForTheme =
                    (if (context.isInNightMode()) domLum > 0.8 else domLum <= 0.2)
                ColorUtils.blendARGB(
                    it,
                    colorBack,
                    if (lumWrongForTheme) 0.9f else 0.7f,
                )
            }
    }

    private fun setRefreshStyle() {
        with(binding.swipeRefresh) {
            if (presenter.preferences.themeMangaDetails().get() && accentColor != null && headerColor != null) {
                val newColor = makeColorFrom(
                    hueOf = accentColor!!,
                    satAndLumOf = context.getResourceColor(R.attr.actionBarTintColor),
                )
                setColorSchemeColors(newColor)
                setProgressBackgroundColorSchemeColor(headerColor!!)
            } else {
                setStyle()
            }
        }
    }

    private fun setHeaderColorValue(colorToUse: Int? = null) {
        val context = view?.context ?: return
        val manga = presenter.manga
        headerColor = if (presenter.preferences.themeMangaDetails().get()) {
            (colorToUse ?: manga.vibrantCoverColor)?.let { color ->
                val newColor =
                    makeColorFrom(color, context.getResourceColor(R.attr.colorPrimaryVariant))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || context.isInNightMode()) {
                    activity?.window?.navigationBarColor = ColorUtils.setAlphaComponent(
                        newColor,
                        Color.alpha(activity?.window?.navigationBarColor ?: Color.BLACK),
                    )
                }
                newColor
            }
        } else {
            null
        }
        setRefreshStyle()
    }

    @ColorInt
    private fun makeColorFrom(@ColorInt hueOf: Int, @ColorInt satAndLumOf: Int): Int {
        val satLumArray = FloatArray(3)
        val hueArray = FloatArray(3)
        ColorUtils.colorToHSL(satAndLumOf, satLumArray)
        ColorUtils.colorToHSL(hueOf, hueArray)
        return ColorUtils.HSLToColor(
            floatArrayOf(
                hueArray[0],
                satLumArray[1],
                satLumArray[2],
            ),
        )
    }

    private fun setItemColors() {
        getHeader()?.updateColors()

        if ((adapter?.itemCount ?: 0) > 1) {
            if (isTablet) {
                val chapterHolder = binding.recycler.findViewHolderForAdapterPosition(0) as? MangaHeaderHolder
                chapterHolder?.updateColors()
            }
            presenter.chapters.forEach { chapter ->
                val chapterHolder =
                    binding.recycler.findViewHolderForItemId(chapter.id!!) as? ChapterHolder
                        ?: return@forEach
                chapterHolder.notifyStatus(
                    chapter.status,
                    false,
                    chapter.progress,
                )
            }
        }
    }

    fun setPaletteColor() {
        val view = view ?: return
        if (!presenter.isFolderLoaded()) return
        val manga = presenter.manga

        val request = ImageRequest.Builder(view.context)
            .data(manga.cover())
            .size(SizeResolver.ORIGINAL)
            .allowHardware(false)
            .target(
                onSuccess = { image ->
                    val drawable = image.asDrawable(view.context.resources)

                    val copy = (drawable as? BitmapDrawable)?.let {
                        BitmapDrawable(
                            view.context.resources,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                it.bitmap.copy(Bitmap.Config.HARDWARE, false)
                            } else {
                                it.bitmap.copy(it.bitmap.config!!, false)
                            },
                        )
                    } ?: drawable

                    val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        Palette.from(bitmap).generate { palette ->
                            if (presenter.preferences.themeMangaDetails().get()) {
                                launchUI {
                                    val vibrantColor = palette?.getBestColor() ?: return@launchUI
                                    manga.vibrantCoverColor = vibrantColor
                                    setAccentColorValue(vibrantColor)
                                    setHeaderColorValue(vibrantColor)
                                    setItemColors()
                                }
                            } else {
                                setCoverColorValue()
                                coverColor?.let { color -> getHeader()?.setBackDrop(color) }
                            }
                        }
                    }
                    binding.mangaCoverFull.setImageDrawable(copy)
                    getHeader()?.updateCover(manga)
                },
                onError = {
                    val file = presenter.coverCache.getCoverFile(manga.thumbnail_url, !manga.favorite)
                    if (file != null && file.exists()) {
                        file.delete()
                        setPaletteColor()
                    }
                },
            ).build()
        view.context.imageLoader.enqueue(request)
    }

    private fun addFolderHeader() {
        val tabletHeader = presenter.tabletChapterHeaderItem
        if (tabletHeader != null && tabletAdapter?.scrollableHeaders?.isEmpty() == true) {
            tabletAdapter?.removeAllScrollableHeaders()
            tabletAdapter?.addScrollableHeader(presenter.headerItem)
            adapter?.removeAllScrollableHeaders()
            adapter?.addScrollableHeader(tabletHeader)
        } else if (adapter?.scrollableHeaders?.isEmpty() == true) {
            adapter?.removeAllScrollableHeaders()
            adapter?.addScrollableHeader(presenter.headerItem)
        }
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter?.getItem(position) as? ChapterItem ?: return false
        openChapter(item)
        return false
    }

    private fun openChapter(item: ChapterItem) {
        val context = activity ?: return
        context.startActivity(ReaderActivity.newIntent(context, item.manga, item.chapter))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_details, menu)
        colorToolbar(binding.recycler.canScrollVertically(-1))
        updateMenuVisibility(menu)

        menu.findItem(R.id.download_next)?.title =
            view?.context?.getString(MR.plurals.next_unread_chapters, 1, 1)
        menu.findItem(R.id.download_next_5)?.title =
            view?.context?.getString(MR.plurals.next_unread_chapters, 5, 5)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = activity?.getString(MR.strings.search_chapters)
        if (query.isNotEmpty() && (!searchItem.isActionViewExpanded || searchView.query != query)) {
            searchItem.expandActionView()
            setSearchViewListener(searchView)
            searchView.setQuery(query, true)
            searchView.clearFocus()
        } else {
            setSearchViewListener(searchView)
        }
    }

    private fun setSearchViewListener(searchView: SearchView?) {
        setOnQueryTextChangeListener(searchView) {
            query = it ?: ""
            if (!isTablet) {
                if (query.isNotEmpty()) getHeader()?.collapse() else getHeader()?.expand()
            }
            adapter?.setFilter(query)
            adapter?.performFilter()
            true
        }
    }

    private fun updateMenuVisibility(menu: Menu?) {
        menu ?: return
        menu.findItem(R.id.action_mark_all_as_read)?.isVisible =
            presenter.getNextUnreadChapter() != null
        menu.findItem(R.id.action_mark_all_as_unread)?.isVisible = presenter.anyRead()
        menu.findItem(R.id.action_remove_downloads)?.isVisible = presenter.hasDownloads()
        menu.findItem(R.id.remove_non_bookmarked)?.isVisible = presenter.hasBookmark()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit -> showEditDialog()
            R.id.action_mark_all_as_read -> {
                activity!!.materialAlertDialog()
                    .setMessage(MR.strings.mark_all_chapters_as_read)
                    .setPositiveButton(MR.strings.mark_as_read) { _, _ ->
                        presenter.markChaptersRead(presenter.chapters, true)
                    }
                    .setNegativeButton(AR.string.cancel, null)
                    .show()
            }
            R.id.action_mark_all_as_unread -> {
                activity!!.materialAlertDialog()
                    .setMessage(MR.strings.mark_all_chapters_as_unread)
                    .setPositiveButton(MR.strings.mark_as_unread) { _, _ ->
                        presenter.markChaptersRead(presenter.chapters, false)
                    }
                    .setNegativeButton(AR.string.cancel, null)
                    .show()
            }
            R.id.download_next -> downloadNext(1)
            R.id.download_next_5 -> downloadNext(5)
            R.id.download_unread -> {
                presenter.downloadChapters(presenter.chapters.filter { !it.read })
            }
            R.id.download_all -> presenter.downloadChapters(presenter.chapters)
            R.id.remove_all -> presenter.deleteChapters(presenter.chapters)
            R.id.remove_read -> presenter.deleteChapters(presenter.chapters.filter { it.read })
            R.id.remove_non_bookmarked ->
                presenter.deleteChapters(presenter.chapters.filter { !it.bookmark })
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun downloadNext(count: Int) {
        presenter.downloadChapters(presenter.chapters.filter { !it.read }.take(count))
    }

    private fun showEditDialog() {
        if (!presenter.isFolderLoaded()) return
        editDialog = EditFolderDialog(this, presenter.folder)
        editDialog?.showDialog(router)
    }

    fun changeCover() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, null), REQUEST_COVER)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_COVER && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            pendingCoverUri = uri
            editDialog?.updateCover(uri)
        }
    }

    fun saveFolderEdits(
        name: String?,
        description: String?,
        author: String?,
        artist: String?,
        genre: String?,
        coverUri: Uri?,
        resetCover: Boolean,
    ) {
        presenter.updateMetadata(
            name = name,
            description = description,
            author = author,
            artist = artist,
            genre = genre,
            coverUri = coverUri ?: pendingCoverUri,
            resetCover = resetCover,
        )
        pendingCoverUri = null
    }

    // region MangaDetailsHost
    override fun coverColor(): Int? = coverColor
    override fun accentColor(): Int? = accentColor
    override fun mangaPresenter(): MangaDetailsData = presenter
    override fun topCoverHeight(): Int = headerHeight
    override fun prepareToShareManga() {}
    override fun openInWebView() {}
    override fun showTrackingSheet() {}
    override fun favoriteManga(longPress: Boolean) { showEditDialog() }
    override fun setFavButtonPopup(popupView: View) {}
    override fun zoomImageFromThumb(thumbView: View) {}
    override fun updateScroll() {
        binding.recycler.post { binding.recycler.invalidateItemDecorations() }
    }

    override fun showFloatingActionMode(view: TextView, content: String?, isTag: Boolean) {}
    override fun customActionMode(view: TextView): android.view.ActionMode.Callback =
        object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

    override fun copyContentToClipboard(content: String, label: StringResource, useToast: Boolean) {
        copyContentToClipboard(content, view?.context?.getString(label), useToast)
    }

    override fun copyContentToClipboard(content: String, label: Int, useToast: Boolean) {
        copyContentToClipboard(content, view?.context?.getString(label), useToast)
    }

    override fun copyContentToClipboard(content: String, label: String?, useToast: Boolean) {
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
        if (useToast) {
            activity?.toast(activity!!.getString(MR.strings._copied_to_clipboard, label.orEmpty()))
        }
    }

    override fun showChapterFilter() {
        val context = activity ?: return
        val options = arrayOf(
            context.getString(MR.strings.read),
            context.getString(MR.strings.downloaded),
            context.getString(MR.strings.bookmarked),
            context.getString(MR.strings.free_movement),
        )
        context.materialAlertDialog()
            .setTitle(MR.strings.sort_and_filter)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> presenter.cycleReadFilter()
                    1 -> presenter.cycleDownloadedFilter()
                    2 -> presenter.cycleBookmarkedFilter()
                    3 -> {
                        presenter.toggleFreeMovement()
                        adapter?.isHandleDragEnabled = presenter.freeMovementEnabled
                        adapter?.notifyDataSetChanged()
                    }
                }
                getHeader()?.binding?.filtersText?.text = presenter.currentFilters()
            }
            .show()
    }

    override fun readNextChapter(readingButton: View) {
        val item = presenter.getNextUnreadChapter()
        if (item != null) {
            openChapter(item)
        } else {
            snack = view?.snack(MR.strings.next_chapter_not_found)
        }
    }

    override fun startDownloadRange(position: Int) {}

    override fun downloadChapter(position: Int) {
        val chapter = adapter?.getItem(position) as? ChapterItem ?: return
        presenter.downloadChapter(chapter)
    }

    override fun startDownloadNow(position: Int) {
        val chapter = adapter?.getItem(position) as? ChapterItem ?: return
        presenter.startDownloadNow(chapter)
    }

    override fun bookmarkChapter(position: Int) {
        val item = adapter?.getItem(position) as? ChapterItem ?: return
        presenter.bookmarkChapters(listOf(item), !item.bookmark)
    }

    override fun toggleReadChapter(position: Int) {
        val item = adapter?.getItem(position) as? ChapterItem ?: return
        presenter.markChaptersRead(listOf(item), !item.read)
    }

    override fun dismissPopup(position: Int) {
        chapterPopupMenu?.dismiss()
        chapterPopupMenu = null
    }

    override fun reorderChapters(chapterIds: List<Long>) {
        presenter.reorderChapters(chapterIds)
    }
    // endregion

    // region drag to reorder
    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        binding.swipeRefresh.isEnabled = actionState == ItemTouchHelper.ACTION_STATE_IDLE
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {}

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
        return fromPosition > 0 && toPosition > 0
    }
    // endregion

    companion object {
        const val FOLDER_EXTRA = "folder"
        private const val REQUEST_COVER = 201
    }
}
