package dev.cinderhell.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ContentItemDao {
    @Query("SELECT * FROM content_items ORDER BY importedAtEpochMillis, displayName")
    fun observeAll(): Flow<List<ContentItemEntity>>

    @Query("SELECT * FROM content_items ORDER BY importedAtEpochMillis, displayName")
    suspend fun getAll(): List<ContentItemEntity>

    @Query("SELECT * FROM content_items WHERE contentId = :contentId")
    suspend fun getById(contentId: String): ContentItemEntity?

    @Query("SELECT * FROM content_items WHERE sha256 = :sha256")
    suspend fun getByDigest(sha256: String): ContentItemEntity?

    @Query("SELECT COUNT(*) FROM content_items WHERE blobPath = :blobPath")
    suspend fun countUsingBlob(blobPath: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ContentItemEntity): Long

    @Delete
    suspend fun delete(item: ContentItemEntity)
}

@Dao
internal interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles ORDER BY selected DESC, updatedAtEpochMillis DESC, name")
    fun observeAllWithEntries(): Flow<List<ProfileWithEntries>>

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY selected DESC, updatedAtEpochMillis DESC, name")
    suspend fun getAllWithEntries(): List<ProfileWithEntries>

    @Transaction
    @Query("SELECT * FROM profiles WHERE profileId = :profileId")
    suspend fun getWithEntries(profileId: String): ProfileWithEntries?

    @Query("SELECT * FROM profiles WHERE selected = 1 LIMIT 1")
    suspend fun getSelected(): ProfileEntity?

    @Query(
        """
        SELECT DISTINCT profiles.* FROM profiles
        LEFT JOIN profile_entries ON profile_entries.profileId = profiles.profileId
        WHERE profiles.gameContentId = :contentId
           OR profile_entries.contentId = :contentId
        ORDER BY profiles.name
        """,
    )
    suspend fun getReferencing(contentId: String): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("UPDATE profiles SET selected = 0 WHERE selected = 1")
    suspend fun clearSelection()

    @Query("DELETE FROM profile_entries WHERE profileId = :profileId")
    suspend fun deleteEntries(profileId: String)

    @Query("SELECT * FROM profile_entries WHERE contentId = :contentId")
    suspend fun getEntriesUsingContent(contentId: String): List<ProfileEntryEntity>

    @Query("SELECT * FROM profile_entries WHERE profileId = :profileId ORDER BY loadPosition")
    suspend fun getEntries(profileId: String): List<ProfileEntryEntity>

    @Query("DELETE FROM profile_entries WHERE contentId = :contentId")
    suspend fun deleteEntriesUsingContent(contentId: String)

    @Update
    suspend fun updateEntries(entries: List<ProfileEntryEntity>)

    @Query("SELECT * FROM profiles WHERE gameContentId = :contentId")
    suspend fun getUsingGame(contentId: String): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<ProfileEntryEntity>)

    @Query("DELETE FROM profiles WHERE profileId IN (:profileIds)")
    suspend fun deleteProfiles(profileIds: List<String>)
}

@Dao
internal interface RecentSessionDao {
    @Query(
        """
        SELECT * FROM recent_sessions
        ORDER BY COALESCE(endedAtEpochMillis, startedAtEpochMillis) DESC
        LIMIT 1
        """,
    )
    fun observeLatest(): Flow<RecentSessionEntity?>

    @Query(
        """
        SELECT * FROM recent_sessions
        ORDER BY COALESCE(endedAtEpochMillis, startedAtEpochMillis) DESC
        LIMIT 1
        """,
    )
    suspend fun getLatest(): RecentSessionEntity?

    @Query("SELECT * FROM recent_sessions WHERE recentSessionId = :sessionId")
    suspend fun getById(sessionId: String): RecentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: RecentSessionEntity)

    @Query("DELETE FROM recent_sessions WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
