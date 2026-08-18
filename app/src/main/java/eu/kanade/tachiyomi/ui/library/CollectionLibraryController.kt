package eu.kanade.tachiyomi.ui.library

import android.os.Bundle
import android.view.View
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.isControllerVisible
import uy.kohesive.injekt.injectLazy
import yokai.domain.collection.interactor.GetCollections
import yokai.i18n.MR
import yokai.util.lang.getString

class CollectionLibraryController : LibraryController {

    constructor(collectionId: Long) : super(
        Bundle().apply { putLong(COLLECTION_EXTRA, collectionId) },
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle)

    val collectionId: Long
        get() = args.getLong(COLLECTION_EXTRA)

    private val getCollections: GetCollections by injectLazy()
    private var collectionName: String? = null

    override fun getTitle(): String? {
        return collectionName ?: view?.context?.getString(MR.strings.collections)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        viewScope.launchIO {
            val name = getCollections.awaitById(collectionId)?.name
            withUIContext {
                collectionName = name
                if (isControllerVisible) setTitle()
            }
        }
    }

    companion object {
        const val COLLECTION_EXTRA = "collection"
    }
}
