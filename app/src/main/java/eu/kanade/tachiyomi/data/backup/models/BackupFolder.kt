package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupFolder(
    @ProtoNumber(1) var name: String,
    @ProtoNumber(2) var sort: Int = 0,
    @ProtoNumber(3) var chapters: List<BackupFolderChapter> = emptyList(),
    @ProtoNumber(4) var subfolders: List<BackupFolder> = emptyList(),
    @ProtoNumber(5) var description: String? = null,
    @ProtoNumber(6) var cover: String? = null,
    @ProtoNumber(7) var author: String? = null,
    @ProtoNumber(8) var artist: String? = null,
    @ProtoNumber(9) var genre: String? = null,
)

@Serializable
data class BackupFolderChapter(
    @ProtoNumber(1) var mangaSource: Long,
    @ProtoNumber(2) var mangaUrl: String,
    @ProtoNumber(3) var chapterUrl: String,
)
