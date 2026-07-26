package dev.cinderhell.library

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

internal enum class ContentType {
    GAME_WAD,
    MOD_WAD,
    MOD_ARCHIVE,
    PATCH,
}

internal enum class GameIdentity {
    DOOM,
    DOOM2,
    TNT,
    PLUTONIA,
    FREEDOOM_PHASE1,
    FREEDOOM_PHASE2,
}

internal enum class ProfileEntryKind {
    MOD,
    PATCH,
}

@Entity(
    tableName = "content_items",
    indices = [
        Index(value = ["sha256"], unique = true),
        Index(value = ["blobPath"], unique = true),
    ],
)
internal data class ContentItemEntity(
    @PrimaryKey
    val contentId: String,
    val sha256: String,
    val displayName: String,
    val blobPath: String,
    val byteSize: Long,
    val contentType: ContentType,
    val gameIdentity: GameIdentity?,
    val engineRequirements: String?,
    val importedAtEpochMillis: Long,
    val classificationVersion: Int,
    val bundled: Boolean = false,
)

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = ContentItemEntity::class,
            parentColumns = ["contentId"],
            childColumns = ["gameContentId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("gameContentId"),
        Index("selected"),
    ],
)
internal data class ProfileEntity(
    @PrimaryKey
    val profileId: String,
    val name: String,
    val gameContentId: String,
    val presetId: String,
    val presetVersion: Int,
    val selected: Boolean,
    val configPath: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "profile_entries",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContentItemEntity::class,
            parentColumns = ["contentId"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("profileId"),
        Index("contentId"),
        Index(value = ["profileId", "loadPosition"], unique = true),
        Index(value = ["profileId", "contentId"], unique = true),
    ],
)
internal data class ProfileEntryEntity(
    @PrimaryKey
    val entryId: String,
    val profileId: String,
    val contentId: String,
    val kind: ProfileEntryKind,
    val loadPosition: Int,
)

@Entity(
    tableName = "recent_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("profileId"),
        Index("startedAtEpochMillis"),
    ],
)
internal data class RecentSessionEntity(
    @PrimaryKey
    val recentSessionId: String,
    val profileId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val result: String?,
    val latestLevel: String?,
    val resumableStatePath: String?,
)

internal data class ProfileEntryWithContent(
    @Embedded
    val entry: ProfileEntryEntity,
    @Relation(
        parentColumn = "contentId",
        entityColumn = "contentId",
    )
    val content: ContentItemEntity,
)

internal data class ProfileWithEntries(
    @Embedded
    val profile: ProfileEntity,
    @Relation(
        entity = ProfileEntryEntity::class,
        parentColumn = "profileId",
        entityColumn = "profileId",
    )
    val entries: List<ProfileEntryWithContent>,
) {
    val orderedEntries: List<ProfileEntryWithContent>
        get() = entries.sortedBy { it.entry.loadPosition }
}
