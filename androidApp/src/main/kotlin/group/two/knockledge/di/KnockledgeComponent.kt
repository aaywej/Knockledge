package group.two.knockledge.di

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import group.two.knockledge.KnockledgeController
import group.two.knockledge.KnockledgeGraph
import group.two.knockledge.KnockledgeService
import group.two.knockledge.ai.AiControls
import group.two.knockledge.ai.AndroidModelStorage
import group.two.knockledge.ai.AndroidSpeechEngine
import group.two.knockledge.ai.AndroidTextEngine
import group.two.knockledge.ai.FirebaseAiSummarizer
import group.two.knockledge.ai.FirebaseAiTranscriber
import group.two.knockledge.ai.FirebaseGeminiClient
import group.two.knockledge.ai.HttpModelDownloader
import group.two.knockledge.ai.OnDeviceSummarizer
import group.two.knockledge.ai.OnDeviceTranscriber
import group.two.knockledge.ai.PrefsStore
import group.two.knockledge.ai.SpeechEngine
import group.two.knockledge.ai.SystemModelEngine
import group.two.knockledge.ai.TextEngine
import group.two.knockledge.ai.WhisperTranscriberAdapter
import group.two.knockledge.ai.inferenceThreads
import group.two.knockledge.audio.AndroidAudioSource
import group.two.knockledge.audio.AndroidDeviceAudioSource
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.MixedAudioSource
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.StoredRecentsRepository
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.data.repository.SnapshotNoteSink
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelFiles
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.models.ModelStore
import group.two.knockledge.pipeline.CompositeSummarizer
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.SensitiveFilter
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.InputSources
import group.two.knockledge.settings.KeyValueStore
import org.jetbrains.annotations.Nullable
import javax.inject.Named
import javax.inject.Singleton

/**
 * Process-wide graph, created once by KnockledgeApplication and shared by the activity and the service.
 */
@Singleton
@Component(modules = [KnockledgeModule::class, DataModule::class, KnockRecordingModule::class])
interface KnockledgeComponent {
    /** Auth and notes only, for the network-triggered sync. */
    fun data(): DataGraph

    /** Recording, AI and settings objects the UI drives. */
    fun graph(): KnockledgeGraph

    /** Fills the service's controller and gesture detector; call from onCreate. */
    fun inject(service: KnockledgeService)

    /** Builds the component; one instance per process. */
    @Component.Factory
    interface Factory {
        /** Binds [app] as the component's only external input. */
        fun create(@BindsInstance app: Application): KnockledgeComponent
    }
}

/**
 * The wiring onDeviceGraph used to do by hand: same models, same settings, same pipeline.
 * Android supplies its microphone, "in progress" surface, AI engines, file/HTTP access and a
 * key-value store here; everything else is shared code.
 */
@Module
object KnockledgeModule {

    /** Settings storage backed by SharedPreferences. */
    @Provides
    @Singleton
    fun keyValueStore(app: Application): KeyValueStore = PrefsStore(app)

    /** Total device RAM, used to pick the default summary model. */
    @Provides
    @Named("totalRamBytes")
    fun totalRamBytes(app: Application): Long {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem
    }

    /** Inference thread count derived from the CPU core count. */
    @Provides
    @Named("inferenceThreads")
    fun threads(): Int = inferenceThreads(Runtime.getRuntime().availableProcessors())

    /** App settings, defaulting the summary model to the one this phone's RAM can run. */
    @Provides
    @Singleton
    fun settings(store: KeyValueStore, @Named("totalRamBytes") totalRamBytes: Long): AppSettings =
        AppSettings(store, ModelCatalog.summarizerFor(totalRamBytes).id)

    /** Model files under app-private storage. */
    @Provides
    @Singleton
    fun modelStorage(app: Application): ModelStorage = AndroidModelStorage(app)

    /** Downloads model files over HTTP. */
    @Provides
    @Singleton
    fun modelDownloader(): ModelDownloader = HttpModelDownloader()

    /** Download, install and delete of model files, reporting the model selected in settings. */
    @Provides
    @Singleton
    fun modelStore(storage: ModelStorage, downloader: ModelDownloader, settings: AppSettings): ModelStore =
        ModelStore(storage, downloader) { settings.summaryModel.value }

    /** The same store, exposed through its read-only provider interface. */
    @Provides
    fun modelProvider(store: ModelStore): ModelProvider = store

    /** The same store, exposed for locating installed model files. */
    @Provides
    fun modelFiles(store: ModelStore): ModelFiles = store

    /** Offline Whisper recognition through ONNX Runtime. */
    @Provides
    fun speechEngine(): SpeechEngine = AndroidSpeechEngine

    /** Offline summarization through LiteRT-LM. */
    @Provides
    fun textEngine(): TextEngine = AndroidTextEngine

    /** Null on Android: only iOS 26 ships a built-in model. */
    @Provides
    @Nullable
    fun systemModel(): SystemModelEngine? = null

    /** Whisper transcription, run with the language chosen in settings. */
    @Provides
    @Singleton
    fun onDeviceTranscriber(
        engine: SpeechEngine,
        files: ModelFiles,
        @Named("inferenceThreads") threads: Int,
        settings: AppSettings,
    ): OnDeviceTranscriber = OnDeviceTranscriber(engine, files, threads) { settings.language.value }

    /** Firebase AI while the network is up, on-device Whisper otherwise. */
    @Provides
    @Singleton
    fun transcriber(onDevice: OnDeviceTranscriber, app: Application, settings: AppSettings): Transcriber {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        return CompositeTranscriber(
            online = FirebaseAiTranscriber(FirebaseGeminiClient()),
            offline = WhisperTranscriberAdapter { onDevice.transcribe(it) },
            isOnline = { cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true },
            mode = { settings.transcriptionMode.value },
        )
    }

    /** Summarizes with the model chosen in settings and writes in the chosen recap language. */
    @Provides
    @Singleton
    fun summarizer(
        engine: TextEngine,
        files: ModelFiles,
        @Named("inferenceThreads") threads: Int,
        system: SystemModelEngine?,
        settings: AppSettings,
        app: Application,
    ): Summarizer {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        return CompositeSummarizer(
            online = FirebaseAiSummarizer { settings.summaryLanguage.value },
            offline = OnDeviceSummarizer(
                engine = engine,
                files = files,
                threads = threads,
                system = system,
                selected = { settings.summaryModel.value },
                outputLanguage = { settings.summaryLanguage.value },
            ),
            isOnline = { cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true },
            mode = { settings.comprehensionMode.value },
        )
    }

    /** Regex rules that blank out passwords, numbers and other personal details in transcripts. */
    @Provides
    fun sensitiveFilter(): SensitiveFilter = RegexSensitiveFilter()

    /** Rewind history, kept in key-value storage across restarts. */
    @Provides
    @Singleton
    fun recents(store: KeyValueStore): RecentsRepository = StoredRecentsRepository(store)

    /** Microphone input. */
    @Provides
    @Singleton
    fun micSource(app: Application): AndroidAudioSource = AndroidAudioSource(app)

    /** Sound playing on the phone (playback capture), if this Android version supports it. */
    @Provides
    @Singleton
    @Nullable
    fun deviceSource(app: Application): AndroidDeviceAudioSource? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AndroidDeviceAudioSource(app) else null

    /** Microphone alone, or mixed with phone playback when the user chose that input source. */
    @Provides
    @Singleton
    fun audioSource(
        mic: AndroidAudioSource,
        device: AndroidDeviceAudioSource?,
        settings: AppSettings,
    ): AudioSource = MixedAudioSource(mic, device) {
        settings.inputSource.value == InputSources.MIC_AND_DEVICE
    }

    /** Starts the foreground service when a session starts and stops it when the session ends. */
    @Provides
    @Singleton
    fun sessionHooks(app: Application): SessionHooks = ForegroundServiceHooks(app)

    /** Transcribe, then filter, then summarize. */
    @Provides
    @Singleton
    fun pipeline(transcriber: Transcriber, filter: SensitiveFilter, summarizer: Summarizer): RewindPipeline =
        RewindPipeline(transcriber, filter, summarizer)

    /** Saves a finished rewind as a note for the signed-in user, and skips it when signed out. */
    @Provides
    @Singleton
    fun noteSink(notes: NoteRepository, auth: AuthRepository): NoteSink = SnapshotNoteSink(notes, auth)

    /** The session controller, with the buffer window taken from settings. */
    @Provides
    @Singleton
    fun controller(
        audioSource: AudioSource,
        hooks: SessionHooks,
        pipeline: RewindPipeline,
        recents: RecentsRepository,
        buffer: LocalAudioRingBuffer,
        sink: NoteSink,
        settings: AppSettings,
    ): KnockledgeController =
        KnockledgeController(
            audioSource,
            hooks,
            pipeline,
            recents,
            buffer,
            sink,
            settings.bufferDurationSeconds,
            settings.minAudioSaveSeconds,
        )

    /** Settings and model picker, told at creation whether this device supports playback capture. */
    @Provides
    @Singleton
    fun aiControls(
        settings: AppSettings,
        store: ModelStore,
        system: SystemModelEngine?,
        @Named("totalRamBytes") totalRamBytes: Long,
        device: AndroidDeviceAudioSource?,
    ): AiControls = AiControls(settings, store, system, totalRamBytes, supportsDeviceAudio = device != null)

    /** Everything the UI needs, in one object. */
    @Provides
    @Singleton
    fun graph(
        controller: KnockledgeController,
        recents: RecentsRepository,
        models: ModelProvider,
        ai: AiControls,
    ): KnockledgeGraph = KnockledgeGraph(controller, recents, models, ai)
}

/** Keeps the mic alive in the background and puts Knockledge on the lock screen. */
private class ForegroundServiceHooks(private val context: Context) : SessionHooks {
    override fun onSessionStarted() {
        context.startForegroundService(Intent(context, KnockledgeService::class.java))
    }

    override fun onSessionStopped() {
        context.stopService(Intent(context, KnockledgeService::class.java))
    }
}
