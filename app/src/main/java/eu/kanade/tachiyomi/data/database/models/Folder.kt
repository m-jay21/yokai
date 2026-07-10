package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Folder : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var chapterOrder: List<Long>

    var parentId: Long?

    var description: String?

    var cover: String?

    var author: String?

    var artist: String?

    var genre: String?

    fun chapterOrderToString(): String = chapterOrder.joinToString("/")

    companion object {
        fun create(name: String, parentId: Long? = null): Folder = FolderImpl().apply {
            this.name = name
            this.parentId = parentId
        }

        fun chapterOrderFromString(orderString: String?): List<Long> {
            if (orderString.isNullOrBlank()) return emptyList()
            return orderString.split("/").mapNotNull { it.toLongOrNull() }
        }

        fun mapper(
            id: Long,
            name: String,
            sort: Long,
            orderString: String,
            parentId: Long?,
            description: String?,
            cover: String?,
            author: String?,
            artist: String?,
            genre: String?,
        ) = create(name, parentId).also {
            it.id = id.toInt()
            it.name = name
            it.order = sort.toInt()
            it.chapterOrder = chapterOrderFromString(orderString)
            it.description = description
            it.cover = cover
            it.author = author
            it.artist = artist
            it.genre = genre
        }
    }
}
