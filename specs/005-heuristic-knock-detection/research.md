# Research: Heuristic Knock Detection

## Technical Context

- **Platform**: Android SDK 35, Min SDK 26
- **Language**: Kotlin 2.0+
- **Architecture**: Clean MVVM, Dagger DI
- **Sensors**: `Sensor.TYPE_ACCELEROMETER` (50–100 Hz / `SENSOR_DELAY_GAME`)
- **Key Modules**: `group.two.knockledge.sensor`
- **Verification Target**: JVM unit tests (`./gradlew test`) + on-device evaluation

---

## Decisions & Rationale

### Decision 1: Pure Heuristic Pipeline Isolation

- **Choice**: Implement heuristic knock detection without requiring TensorFlow Lite, neural network models, or gyroscope sensors.
- **Rationale**:
  - The Ponytail principle demands the simplest working solution.
  - The non-machine-learning pipeline relies strictly on linear acceleration derivatives (jerk) and frequency filtering along the Z-axis.
  - Gyroscopes and TFLite runtimes consume significant battery and require bundled assets (`tap7cls_coral.tflite`).
  - An isolated heuristic detector provides a clean baseline to benchmark accuracy before considering ML complexity.
- **Alternatives Considered**:
  - *Keep ML and Heuristic bundled in `AndroidKnockGestureDetector`*: Rejected because it forces asset loading and fails on devices without gyroscope hardware.
  - *Naive single-threshold spike detector*: Rejected because simple thresholding produces excessive false positives when dropping the phone or sitting down.

---

### Decision 2: Signal Processing Mathematical Sequence

- **Choice**: Adopt TapTap's five-stage heuristic pipeline:
  1. **Resampling**: `Resample3C` normalizes irregular Android sensor timestamps to fixed intervals.
  2. **Jerk Calculation (Slope)**: `Slope3C` computes $\Delta a / \Delta t$, isolating impulse velocity changes from static gravitational acceleration.
  3. **Bandpass Filtering**: `Lowpass1C` ($\text{para} = 0.2$) followed by `Highpass1C` ($\text{para} = 0.2$) on the Z-axis extracts knock vibration frequencies.
  4. **Dual Peak & Rebound Verification**: Positive `PeakDetector` detects impact compression; negative `PeakDetector` detects elastic rebound. A knock is valid if and only if the negative rebound peak follows 1 to 2 sample intervals after the positive peak.
  5. **Cadence Gating**: Double knock timing accepts two validated impulses separated by $100\text{ ms} \le \Delta t \le 500\text{ ms}$.
- **Rationale**: Matches physical dynamics of device casing impacts while rejecting handling noise.
- **Alternatives Considered**:
  - *FFT / Frequency Domain Analysis*: High CPU and RAM overhead, violates hackathon constraints.

---

### Decision 3: Evaluation Mode & Metrics Collection

- **Choice**: Expose an optional debug/evaluation listener interface (`HeuristicEvaluationListener`) reporting raw peak amplitudes, rebound timing intervals, and rejection causes.
- **Rationale**: User Story 3 specifically gates ML adoption on heuristic accuracy satisfaction (>= 85% true positive rate, < 2 false triggers per hour). Telemetry enables objective evaluation before writing any machine-learning code.
- **Alternatives Considered**:
  - *Ad-hoc logcat parsing*: Error-prone and inconvenient for systematic verification.

---

### Decision 4: Fallback Architecture for Escalation to ML

- **Choice**: Define a common `KnockGestureDetector` interface so the system can swap between `HeuristicKnockGestureDetector` and `ColumbusMlKnockGestureDetector` without touching UI or ViewModel layers.
- **Rationale**: Respects Karpathy discipline and MVVM boundary. If the user decides heuristic detection is insufficient, switching to ML requires zero changes to `KnockRecordingViewModel` or `KnockRecordingController`.
