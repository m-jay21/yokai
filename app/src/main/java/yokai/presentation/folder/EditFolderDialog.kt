package yokai.presentation.folder

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import coil3.load
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Folder
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.view.setPositiveButton
import yokai.i18n.MR
import java.io.File
import android.R as AR

class EditFolderDialog : DialogController {

    private val folder: Folder
    private var customCoverUri: Uri? = null
    private var willResetCover = false
    lateinit var binding: EditMangaDialogBinding

    private val infoController
        get() = targetController as FolderDetailsController

    constructor(target: FolderDetailsController, folder: Folder) : super(
        Bundle().apply { putLong(KEY_FOLDER, folder.id!!.toLong()) },
    ) {
        targetController = target
        this.folder = folder
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        folder = Folder.create("").also { it.id = bundle.getLong(KEY_FOLDER).toInt() }
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = EditMangaDialogBinding.inflate(activity!!.layoutInflater)
        val dialog = activity!!.materialAlertDialog().apply {
            setView(binding.root)
            setNegativeButton(AR.string.cancel, null)
            setPositiveButton(MR.strings.save) { _, _ -> onPositiveButtonClick() }
        }
        onViewCreated()
        val updateScrollIndicators = {
            binding.scrollIndicatorDown.isVisible = binding.scrollView.canScrollVertically(1)
        }
        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateScrollIndicators()
        }
        binding.scrollView.post { updateScrollIndicators() }
        return dialog.create()
    }

    private fun onViewCreated() {
        binding.mangaStatus.isVisible = false
        binding.seriesType.isVisible = false
        binding.resetsReadingMode.isVisible = false
        binding.mangaLang.isVisible = false
        binding.mangaGenresTags.isVisible = false
        binding.resetTags.isVisible = false
        binding.addTagChip.isVisible = false
        binding.addTagEditText.isVisible = false

        binding.title.append(folder.name)
        binding.mangaAuthor.append(folder.author.orEmpty())
        binding.mangaArtist.append(folder.artist.orEmpty())
        binding.mangaDescription.append(folder.description.orEmpty())

        val cover = folder.cover?.takeIf { it.isNotBlank() }
        if (cover != null) {
            val file = File(cover)
            if (file.exists()) {
                binding.mangaCover.load(file)
            } else {
                binding.mangaCover.load(cover)
            }
        }

        binding.coverLayout.setOnClickListener { infoController.changeCover() }
        binding.resetCover.isVisible = !cover.isNullOrBlank()
        binding.resetCover.setOnClickListener {
            binding.mangaCover.setImageResource(R.mipmap.ic_launcher)
            customCoverUri = null
            willResetCover = true
        }
    }

    fun updateCover(uri: Uri) {
        willResetCover = false
        binding.mangaCover.load(uri)
        customCoverUri = uri
        binding.resetCover.isVisible = true
    }

    private fun onPositiveButtonClick() {
        infoController.saveFolderEdits(
            name = binding.title.text?.toString(),
            description = binding.mangaDescription.text?.toString(),
            author = binding.mangaAuthor.text?.toString(),
            artist = binding.mangaArtist.text?.toString(),
            genre = folder.genre,
            coverUri = customCoverUri,
            resetCover = willResetCover,
        )
    }

    private companion object {
        const val KEY_FOLDER = "folder_id"
    }
}
