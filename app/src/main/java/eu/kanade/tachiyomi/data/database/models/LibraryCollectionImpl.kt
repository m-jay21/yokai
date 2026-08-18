package eu.kanade.tachiyomi.data.database.models

class LibraryCollectionImpl : LibraryCollection {

    override var id: Int? = null

    override lateinit var name: String

    override var order: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return name == (other as LibraryCollection).name
    }

    override fun hashCode(): Int = name.hashCode()
}
