# Research & Technology Decisions: Audio Pre-Processing Pipeline Unit

**Feature**: `008-audio-preprocessing`  
**Date**: 2026-10-31  
**Status**: Completed

## 1. Dead Air Eradication & Speech Activity Detection (VAD)

### Decision
Implement a pure-Kotlin energy-based Voice Activity Detector (VAD) with frame-based Root-Mean-Square (RMS) calculation and a hangover buffer.

### Rationale
- Operates directly on raw 16 kHz 16-bit PCM `ShortArray` buffers without format conversions.
- Eliminates native C++ binaries or external dependencies (such as WebRTC VAD or TensorFlow Lite models), adhering directly to Ponytail Simplicity and Karpathy Discipline.
- Uses 20 ms frames (320 samples at 16 kHz).
- Distinguishes speech from silence using an empirical RMS threshold (~250-350 for 16-bit PCM).
- Preserves natural speech transitions by maintaining a 300 ms hangover padding before and after detected speech frames.
- Truncates silent gaps exceeding 1.5 seconds down to 300 ms of ambient silence, effectively eradicating dead air while preventing unnatural audio clipping.

### Alternatives Considered
- **WebRTC Native VAD**: Requires JNI / NDK native binaries for multiple ABIs (armeabi-v7a, arm64-v8a, x86_64). Rejected due to build complexity, binary footprint, and hackathon scope limits.
- **On-Device ML VAD (Silero / LiteRT)**: Unnecessary resource overhead for 5-minute pre-transcription batch processing. Rejected due to latency and memory consumption.
- **Simple Zero-Crossing Rate (ZCR)**: Too sensitive to high-frequency noise and mic hiss without energy combination. Rejected in favor of RMS energy with hangover.

---

## 2. Audio Normalization

### Decision
Apply peak amplitude normalization with dynamic headroom limiting.

### Rationale
- Scans the speech-retained PCM `ShortArray` for the peak sample value (`maxPeak = max(|sample|)`).
- Computes gain scaling factor `gain = targetPeak / maxPeak`, where `targetPeak = 0.9 * Short.MAX_VALUE` (~29,490, providing ~-1 dBFS headroom).
- Applies gain uniformly across all speech samples, clamping output within `[Short.MIN_VALUE, Short.MAX_VALUE]`.
- Ensures quiet speakers are boosted to clear AI transcription levels while avoiding clipping distortion or digital overdrive.
- O(N) complexity in a single array pass; execution takes under 10 ms on a 5-minute buffer.

### Alternatives Considered
- **EBU R128 Loudness Normalization (LUFS)**: Computationally heavy multi-filter algorithm designed for broadcast streaming. Over-engineered for local speech-to-text input. Rejected.
- **Dynamic Range Compression / AGC**: Alters natural speech dynamics and may amplify low-level background noise during pauses. Rejected in favor of dead air removal combined with linear peak normalization.

---

## 3. Empty Audio & 1.0-Second Speech Threshold Handling

### Decision
Calculate the total duration of active speech frames retained post-dead-air eradication. If the retained speech is less than 1.0 second (16,000 samples at 16 kHz), return an `AudioPreprocessResult.InsufficientSpeech` result.

### Rationale
- User explicit decision: audio captures can occur at the 5-second minimum buffer window. If the entire window is silence or brief transient noise, passing it to Whisper or Gemini results in hallucinations, wasted API tokens, or empty transcripts.
- When `InsufficientSpeech` occurs:
  1. Skip AI transcription and summarization entirely.
  2. Skip note creation and local database persistence (`recents.add` and `NoteRepository` are not called).
  3. Inform the user by publishing a clear error message (`"No speech detected in recorded audio."`) through `KnockledgeController.reportError()`.
  4. Ensure no new Jetpack Compose components are introduced, leveraging the existing state error channel.

### Alternatives Considered
- **Passing Minimal Silence to AI**: Leads to model hallucinations (Whisper outputs "Thank you for watching" or repetitive loops on silence). Rejected.
- **Saving Empty Audio Notes**: Clutters user note history with 0-word empty transcript cards. Rejected.

---

## 4. Preservation of Original Audio Recording

### Decision
The `AudioPreprocessor` strictly generates a new in-memory `ShortArray` buffer for the AI rewind pipeline. The original WAV snapshot file produced by `buffer.snapshot()` remains completely untouched.

### Rationale
- The original recorded WAV file is passed to `noteSink.record(wav, ...)` without modification.
- User playback retains authentic conversational timing, room acoustics, and full unaltered context.
- Satisfies the critical constraint: "note that the original file must remain intact. the pre processing is only for the rewind pipeline".

### Alternatives Considered
- **Overwriting the Original WAV with Pre-Processed Audio**: Modifies user historical audio, cutting real pauses during playback. Rejected per user requirement.

---

## 5. Removal of Filtering Unit from RewindPipeline

### Decision
Remove `SensitiveFilter` and `RegexSensitiveFilter` dependencies from `RewindPipeline`, `KnockledgeComponent`, and `KnockledgeGraph`. Pass transcribed text directly to the summarization stage with `redactedCount = 0`.

### Rationale
- The user explicitly requested: "completely remove filtering unit of the rewind pipeline".
- Eliminates regex scanning overhead on transcript strings.
- Direct transcript result flows straight to `Summarizer`.

### Alternatives Considered
- **Retaining SensitiveFilter as a Passthrough No-Op**: Retains dead abstractions in the pipeline signature. Rejected in favor of surgical deletion per Karpathy and Ponytail directives.
