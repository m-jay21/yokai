package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface LibraryCollection : Serializable {

    var id: Int?

    var name: String

    var order: Int

    companion object {
        fun create(name: String): LibraryCollection = LibraryCollectionImpl().apply {
            this.name = name
        }

        fun mapper(
            id: Long,
            name: String,
            sort: Long,
        ) = create(name).also {
            it.id = id.toInt()
            it.name = name
            it.order = sort.toInt()
        }
    }
}
