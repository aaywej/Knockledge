# Quickstart & Verification Guide: Custom Notification Layout

## Verification Flow

### Prerequisites
- Android device or emulator running API 26+ (Android 8.0+).
- Application installed via `./gradlew :androidApp:assembleDebug` or IDE run.

### Manual Verification Steps

1. **Start Recording**:
   - Launch Knockledge app.
   - Tap "Start Recording".
   - Pull down Android notification shade or lock screen.
   - **Expected**: Ongoing notification shows custom layout with live `00:00` timer ticking upward and save count badge showing `0 saved`.

2. **Trigger Rewind from Notification**:
   - Allow recording to run past the minimum unlock time (e.g., 5-10 seconds).
   - Tap the **Rewind** action button directly on the notification card.
   - **Expected**: Vibrates/confirms clip save; save count badge on notification immediately updates to `1 saved`.

3. **Trigger Stop from Notification**:
   - Tap the **Stop** action button directly on the notification card.
   - **Expected**: Recording stops immediately, service stops, and notification is removed from the shade.

4. **Background / Lock Screen Visibility**:
   - Lock screen device while recording.
   - Wake screen to observe lock screen notification widget.
   - **Expected**: High-contrast, clean layout displaying live timer, save count, Rewind, and Stop buttons.

### Automated Unit Test Command
```powershell
./gradlew :androidApp:testDebugUnitTest
```
Runs unit tests validating `KnockledgeService` state mapping and `KnockledgeController` save count updates.
