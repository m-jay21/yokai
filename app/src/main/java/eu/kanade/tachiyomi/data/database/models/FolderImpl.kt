package eu.kanade.tachiyomi.data.database.models

class FolderImpl : Folder {

    override var id: Int? = null

    override lateinit var name: String

    override var order: Int = 0

    override var chapterOrder: List<Long> = emptyList()

    override var parentId: Long? = null

    override var description: String? = null

    override var cover: String? = null

    override var author: String? = null

    override var artist: String? = null

    override var genre: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return name == (other as Folder).name
    }

    override fun hashCode(): Int = name.hashCode()
}
