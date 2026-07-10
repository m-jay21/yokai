package yokai.domain.folder.models

data class FolderUpdate(
    val id: Long,
    val name: String? = null,
    val chapterOrder: String? = null,
    val order: Long? = null,
    val parentId: Long? = null,
    val clearParent: Boolean = false,
    val description: String? = null,
    val cover: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val genre: String? = null,
)
