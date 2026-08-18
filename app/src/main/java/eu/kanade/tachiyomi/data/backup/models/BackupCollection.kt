package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupCollection(
    @ProtoNumber(1) var name: String,
    @ProtoNumber(2) var sort: Int = 0,
    @ProtoNumber(3) var manga: List<BackupCollectionManga> = emptyList(),
)

@Serializable
data class BackupCollectionManga(
    @ProtoNumber(1) var mangaSource: Long,
    @ProtoNumber(2) var mangaUrl: String,
)
