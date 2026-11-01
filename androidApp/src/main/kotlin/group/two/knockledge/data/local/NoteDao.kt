package group.two.knockledge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Local notes table; every query filters by owner so one account never sees another's notes. */
@Dao
interface NoteDao {
    /** Emits the user's notes newest first and re-emits whenever the table changes. */
    @Query("SELECT * FROM notes WHERE user_id = :userId ORDER BY created_at_epoch_ms DESC")
    fun observeNotes(userId: String): Flow<List<NoteEntity>>

    /** Returns the note if it exists and belongs to the user, otherwise null. */
    @Query("SELECT * FROM notes WHERE id = :id AND user_id = :userId")
    suspend fun getNote(id: String, userId: String): NoteEntity?

    /** Inserts the note or replaces the row with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    /** Deletes the row only if it belongs to the user; the audio file is left on disk. */
    @Query("DELETE FROM notes WHERE id = :id AND user_id = :userId")
    suspend fun delete(id: String, userId: String)
}

/** Pending cloud work; a task stays queued until it succeeds or is dropped. */
@Dao
interface SyncQueueDao {
    /** Inserts a task or overwrites the existing one with the same id, used for status updates. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    /** Tasks for the user that are not being processed right now, oldest update first. */
    @Query("SELECT * FROM sync_queue WHERE user_id = :userId AND status != 'PROCESSING' ORDER BY updated_at_epoch_ms")
    suspend fun pending(userId: String): List<SyncQueueEntity>

    /** Removes a task once it has been handled or abandoned. */
    @Query("DELETE FROM sync_queue WHERE queue_id = :queueId")
    suspend fun delete(queueId: String)
}
