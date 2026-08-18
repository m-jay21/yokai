package yokai.domain.collection.models

data class CollectionUpdate(
    val id: Long,
    val name: String? = null,
    val order: Long? = null,
)
