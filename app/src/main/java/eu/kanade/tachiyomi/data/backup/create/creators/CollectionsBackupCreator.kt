package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupCollection
import eu.kanade.tachiyomi.data.backup.models.BackupCollectionManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.collection.interactor.GetCollections
import yokai.domain.manga.interactor.GetManga

class CollectionsBackupCreator(
    private val getCollections: GetCollections = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) {
    suspend operator fun invoke(): List<BackupCollection> {
        return getCollections.await().map { collection ->
            val collectionId = collection.id!!.toLong()
            val mangaRefs = getCollections.awaitMangaIds(collectionId).mapNotNull { mangaId ->
                val manga = getManga.awaitById(mangaId) ?: return@mapNotNull null
                BackupCollectionManga(
                    mangaSource = manga.source,
                    mangaUrl = manga.url,
                )
            }
            BackupCollection(
                name = collection.name,
                sort = collection.order,
                manga = mangaRefs,
            )
        }
    }
}
