# Research: Audio Playback with Interactive Chapters

This document records architectural decisions for implementing audio playback, interactive chapter navigation, and chapter generation.

## Decision 1: Playback Engine Selection

### Decision
Use Android platform `android.media.MediaPlayer` wrapped in an `AudioPlayer` interface.

### Rationale
- Native Android SDK class with zero additional dependencies.
- Natively decodes and plays 16 kHz uncompressed PCM WAV files stored in [`AudioFileStorage`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/local/AudioFileStorage.kt#L7-L10).
- Natively supports streaming from remote HTTPS URLs (such as Firebase Storage audio URLs).
- Adheres strictly to the Ponytail simplicity principle and the 3-day hackathon MVP timeline.

### Alternatives Considered
- **Jetpack Media3 `ExoPlayer`**: Adds multiple external dependencies (`androidx.media3:media3-exoplayer`), increases APK size, and introduces unnecessary complexity for short 1-to-5 minute WAV files.
- **Low-Level `AudioTrack`**: Requires writing custom WAV file header parsers and manual PCM streaming thread loops, increasing boilerplate and failure points.

---

## Decision 2: Playback State Architecture & Coroutine Polling

### Decision
Model playback state as an immutable `AudioPlaybackState` data class exposed via `StateFlow<AudioPlaybackState>`. Use a coroutine ticker loop on `Dispatchers.Main` that polls `mediaPlayer.currentPosition` every 100 ms while audio plays.

### Rationale
- Adheres to Unidirectional Data Flow (UDF) standards mandated in the project constitution.
- Decouples Jetpack Compose UI from Android `MediaPlayer` lifecycle and error states.
- `MediaPlayer` lacks a native continuous position change listener; 100 ms polling provides smooth seekbar updates (10 updates per second) with negligible CPU overhead.
- Coroutine ticker automatically pauses when audio stops or completes, eliminating battery drain.

### Alternatives Considered
- **Compose `LaunchedEffect` Polling**: Couples playback timing to UI composition lifecycle; ceases updating if UI navigates away while background audio continues.
- **Java `Timer` or `Handler.postDelayed`**: Outdated Android pattern; fails to cancel cleanly within modern coroutine scopes.

---

## Decision 3: YouTube-Style Chapter Scrubbing & Visual Marks

### Decision
Render chapter segments by calculating progress percentage for each [`ChapterMarker`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/model/NoteModels.kt#L62-L66) relative to total audio duration. When a user taps a chapter in the chapter list or seekbar, call `audioPlayer.seekTo(marker.startTimeMs)`. Compute the active chapter reactively by finding the marker with the largest `startTimeMs <= currentPositionMs`.

### Rationale
- Provides immediate visual parity with YouTube video player chapters.
- Pure Compose math without custom Canvas complexity.
- Reuses the existing `startTimeMs` attribute on [`ChapterMarker`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/model/NoteModels.kt#L62-L66).

### Alternatives Considered
- **Separate Third-Party Video Player View**: Overhead of embedding a video player component for audio-only content.
- **Text-Only Chapter List**: Fails to provide visual scrub bar divisions on the player timeline.

---

## Decision 4: Automated Chapter Marker Generation in AI Pipeline

### Decision
Update the AI summarization prompt in [`FirebaseAiTranscriber`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/ai/FirebaseAiTranscriber.kt) to extract topic segments and produce timestamped chapter markers. Forward these markers through [`SnapshotNoteSink`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/repository/SnapshotNoteSink.kt#L79) into [`NoteRepository.attachSummary()`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/NoteRepository.kt).

### Rationale
- The data layer ([`NoteRepository`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/NoteRepository.kt), [`NoteEntity`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/local/NoteEntity.kt), Room converters, and Firestore sync) already contains full support for `ChapterMarker`.
- Updating the AI pipeline completes the end-to-end user experience without database migrations or schema adjustments.

### Alternatives Considered
- **Fixed-Interval Chapter Splitting**: Arbitrarily splitting audio every 60 seconds ignores semantic topic changes and produces nonsensical chapter boundaries.
