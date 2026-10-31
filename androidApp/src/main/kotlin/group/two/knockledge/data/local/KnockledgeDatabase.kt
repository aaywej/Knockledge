package group.two.knockledge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** Room database for notes and the queue of work still owed to the cloud. */
@Database(entities = [NoteEntity::class, SyncQueueEntity::class], version = 1, exportSchema = false)
@TypeConverters(NoteTypeConverters::class)
abstract class KnockledgeDatabase : RoomDatabase() {
    /** Notes table access; every read and write is scoped by user id. */
    abstract fun noteDao(): NoteDao

    /** Queue of upload and Firestore tasks waiting to run. */
    abstract fun syncQueueDao(): SyncQueueDao
}
