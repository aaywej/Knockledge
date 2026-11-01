package group.two.knockledge.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.local.AudioFileStorage
import group.two.knockledge.data.local.KnockledgeDatabase
import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.remote.AudioUploads
import group.two.knockledge.data.remote.AuthSource
import group.two.knockledge.data.remote.FirebaseAuthSource
import group.two.knockledge.data.remote.FirebaseStorageSource
import group.two.knockledge.data.remote.FirestoreDataSource
import group.two.knockledge.data.remote.NoteSyncSource
import group.two.knockledge.data.repository.AuthRepositoryImpl
import group.two.knockledge.data.repository.NoteRepositoryImpl
import group.two.knockledge.data.repository.SyncEngine
import javax.inject.Inject
import javax.inject.Singleton

/** The data-layer dependencies handed out together, so callers need not inject auth and notes separately. */
class DataGraph @Inject constructor(val auth: AuthRepository, val notes: NoteRepository)

/** Room, Firebase and the repositories built on top of them. */
@Module
object DataModule {
    /** The single app database, opened on first use. */
    @Provides
    @Singleton
    fun database(app: Application): KnockledgeDatabase =
        Room.databaseBuilder(app, KnockledgeDatabase::class.java, "knockledge.db").build()

    /** Local note rows. */
    @Provides
    fun noteDao(db: KnockledgeDatabase): NoteDao = db.noteDao()

    /** Queued uploads and document writes waiting to reach the backend. */
    @Provides
    fun syncQueueDao(db: KnockledgeDatabase): SyncQueueDao = db.syncQueueDao()

    /** Firebase Auth as the sign-in backend. */
    @Provides
    @Singleton
    fun authSource(): AuthSource = FirebaseAuthSource()

    /** The signed-in user exposed as state. */
    @Provides
    @Singleton
    fun authRepository(source: AuthSource): AuthRepository = AuthRepositoryImpl(source)

    /** Firebase Storage, which receives note audio. */
    @Provides
    @Singleton
    fun audioUploads(): AudioUploads = FirebaseStorageSource()

    /** Firestore, which receives the note documents. */
    @Provides
    @Singleton
    fun noteSyncSource(): NoteSyncSource = FirestoreDataSource()

    /** Drains this user's queue, retrying each task with exponential backoff up to four attempts. */
    @Provides
    @Singleton
    fun syncEngine(
        notes: NoteDao,
        queue: SyncQueueDao,
        uploads: AudioUploads,
        remote: NoteSyncSource,
    ): SyncEngine = SyncEngine(notes, queue, uploads, remote)

    /** Notes written to Room with audio under app storage; every write also queues a sync task. */
    @Provides
    @Singleton
    fun noteRepository(
        app: Application,
        notes: NoteDao,
        queue: SyncQueueDao,
        engine: SyncEngine,
    ): NoteRepository =
        NoteRepositoryImpl(notes, queue, AudioFileStorage.audioDir(app), onSync = engine::syncUser)
}
