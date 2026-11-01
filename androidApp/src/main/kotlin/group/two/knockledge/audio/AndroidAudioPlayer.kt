package group.two.knockledge.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import group.two.knockledge.data.model.ChapterMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android platform implementation of [AudioPlayer] wrapping [MediaPlayer].
 */
@Singleton
class AndroidAudioPlayer @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val mediaPlayerFactory: () -> MediaPlayer = { MediaPlayer() }
) : AudioPlayer, AudioManager.OnAudioFocusChangeListener {

    private val _state = MutableStateFlow(AudioPlaybackState())
    override val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null
    private var chaptersList: List<ChapterMarker> = emptyList()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun play(file: File, chapters: List<ChapterMarker>) {
        if (!file.exists()) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = "File not found: ${file.absolutePath}"
                )
            }
            return
        }
        chaptersList = chapters.sortedBy { it.startTimeMs }
        setupAndPlay { player ->
            player.setDataSource(file.absolutePath)
            player.prepare()
        }
    }

    override fun play(url: String, chapters: List<ChapterMarker>) {
        chaptersList = chapters.sortedBy { it.startTimeMs }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        setupAndPlay(isAsync = true) { player ->
            player.setDataSource(url)
            player.prepareAsync()
        }
    }

    private fun setupAndPlay(isAsync: Boolean = false, prepareAction: (MediaPlayer) -> Unit) {
        stopInternal()
        requestAudioFocus()

        try {
            val player = mediaPlayerFactory()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            player.setOnCompletionListener {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        currentPositionMs = it.durationMs,
                        activeChapter = computeActiveChapter(it.durationMs, chaptersList)
                    )
                }
                stopTicker()
            }

            player.setOnErrorListener { _, what, extra ->
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = "Playback error ($what, $extra)"
                    )
                }
                stopTicker()
                true
            }

            if (isAsync) {
                player.setOnPreparedListener { mp ->
                    startPlaybackAfterPrepared(mp)
                }
                prepareAction(player)
            } else {
                prepareAction(player)
                startPlaybackAfterPrepared(player)
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to initialize playback"
                )
            }
        }
    }

    private fun startPlaybackAfterPrepared(player: MediaPlayer) {
        val duration = player.duration.coerceAtLeast(0).toLong()
        player.start()
        val currentPos = player.currentPosition.coerceAtLeast(0).toLong()
        _state.update {
            it.copy(
                isPlaying = true,
                isLoading = false,
                durationMs = duration,
                currentPositionMs = currentPos,
                activeChapter = computeActiveChapter(currentPos, chaptersList),
                errorMessage = null
            )
        }
        startTicker()
    }

    override fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            val currentPos = player.currentPosition.coerceAtLeast(0).toLong()
            _state.update {
                it.copy(
                    isPlaying = false,
                    currentPositionMs = currentPos,
                    activeChapter = computeActiveChapter(currentPos, chaptersList)
                )
            }
            stopTicker()
        }
    }

    override fun resume() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            requestAudioFocus()
            player.start()
            val currentPos = player.currentPosition.coerceAtLeast(0).toLong()
            _state.update {
                it.copy(
                    isPlaying = true,
                    currentPositionMs = currentPos,
                    activeChapter = computeActiveChapter(currentPos, chaptersList)
                )
            }
            startTicker()
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = _state.value.durationMs
        val targetPos = positionMs.coerceIn(0L, duration.coerceAtLeast(0L))
        mediaPlayer?.let { player ->
            try {
                player.seekTo(targetPos.toInt())
            } catch (e: Exception) {
                // Ignore seek error if player not ready
            }
        }
        _state.update {
            it.copy(
                currentPositionMs = targetPos,
                activeChapter = computeActiveChapter(targetPos, chaptersList)
            )
        }
    }

    override fun stop() {
        stopInternal()
        _state.value = AudioPlaybackState()
    }

    override fun release() {
        stopInternal()
        abandonAudioFocus()
        _state.value = AudioPlaybackState()
    }

    private fun stopInternal() {
        stopTicker()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                // Ignore release/stop errors
            }
        }
        mediaPlayer = null
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                delay(100)
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.coerceAtLeast(0).toLong()
                        _state.update { state ->
                            state.copy(
                                currentPositionMs = currentPos,
                                activeChapter = computeActiveChapter(currentPos, chaptersList)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun computeActiveChapter(
        positionMs: Long,
        chapters: List<ChapterMarker>
    ): ChapterMarker? {
        if (chapters.isEmpty()) return null
        return chapters.filter { it.startTimeMs <= positionMs }
            .maxByOrNull { it.startTimeMs }
    }

    private fun requestAudioFocus() {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> { }
        }
    }
}
