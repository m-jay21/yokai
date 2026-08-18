package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupCollection
import eu.kanade.tachiyomi.data.database.models.LibraryCollection
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.data.DatabaseHandler
import yokai.domain.collection.CollectionRepository
import yokai.domain.collection.interactor.GetCollections
import yokai.domain.manga.interactor.GetManga

class CollectionsBackupRestorer(
    private val getCollections: GetCollections = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val collectionRepository: CollectionRepository = Injekt.get(),
    private val handler: DatabaseHandler = Injekt.get(),
) {
    suspend fun restoreCollections(backupCollections: List<BackupCollection>, onComplete: () -> Unit) {
        handler.await(true) {
            backupCollections.forEach { backupCollection ->
                restoreCollection(backupCollection)
            }
        }
        onComplete()
    }

    private suspend fun restoreCollection(backupCollection: BackupCollection) {
        val existing = getCollections.await().find { it.name == backupCollection.name }
        val collectionId = if (existing != null) {
            existing.id!!.toLong()
        } else {
            val collection = LibraryCollection.create(backupCollection.name).apply {
                order = backupCollection.sort
            }
            collectionRepository.insert(collection) ?: return
        }

        backupCollection.manga.forEach { ref ->
            val manga = getManga.awaitByUrlAndSource(ref.mangaUrl, ref.mangaSource) ?: return@forEach
            val mangaId = manga.id ?: return@forEach
            collectionRepository.addManga(collectionId, mangaId)
        }
    }
}
