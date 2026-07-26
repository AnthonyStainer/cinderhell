package dev.cinderhell.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ContentItemEntity::class,
        ProfileEntity::class,
        ProfileEntryEntity::class,
        RecentSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(LibraryTypeConverters::class)
internal abstract class LibraryDatabase : RoomDatabase() {
    abstract fun contentItems(): ContentItemDao
    abstract fun profiles(): ProfileDao
    abstract fun recentSessions(): RecentSessionDao

    companion object {
        const val DATABASE_NAME = "cinderhell-library.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE content_items ADD COLUMN engineRequirements TEXT",
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var instance: LibraryDatabase? = null

        fun get(context: Context): LibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(*MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}

internal class LibraryTypeConverters {
    @TypeConverter
    fun contentTypeToString(value: ContentType): String = value.name

    @TypeConverter
    fun stringToContentType(value: String): ContentType = ContentType.valueOf(value)

    @TypeConverter
    fun gameIdentityToString(value: GameIdentity?): String? = value?.name

    @TypeConverter
    fun stringToGameIdentity(value: String?): GameIdentity? = value?.let(GameIdentity::valueOf)

    @TypeConverter
    fun entryKindToString(value: ProfileEntryKind): String = value.name

    @TypeConverter
    fun stringToEntryKind(value: String): ProfileEntryKind = ProfileEntryKind.valueOf(value)
}
