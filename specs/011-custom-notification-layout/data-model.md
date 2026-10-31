# Data Model & State Schema: Custom Notification Layout

## 1. Notification UI State Schema

The notification model represents the immutable state consumed by `KnockledgeService` when inflating or updating the `RemoteViews` custom layout.

```kotlin
/**
 * Data state rendered by KnockledgeService into RemoteViews.
 */
data class NotificationModel(
    val running: Boolean,
    val startTimeMillis: Long,
    val bufferedSeconds: Long,
    val canRewind: Boolean,
    val summarizing: Boolean,
    val minSaveMillis: Long,
    val millisUntilUnlock: Long,
    val savedCount: Int,
)
```

## 2. Field Specifications & Transformations

| Field | Type | Description | Source / Derivation |
|---|---|---|---|
| `running` | `Boolean` | True if recording session is active | `KnockledgeState.isRunning` |
| `startTimeMillis` | `Long` | Base timestamp for `<Chronometer>` | Calculated at session start (`SystemClock.elapsedRealtime()`) |
| `bufferedSeconds` | `Long` | Audio buffer duration in memory | `KnockledgeState.bufferedMillis / 1000L` |
| `canRewind` | `Boolean` | True when minimum save buffer is ready | `KnockledgeState.canRewind` |
| `summarizing` | `Boolean` | True during AI summary generation | `KnockledgeState.isSummarizing` |
| `savedCount` | `Int` | Total audio clips saved in current session | `KnockledgeState.savedClips.size` (or controller save counter) |

## 3. RemoteViews Binding Mapping

| RemoteViews View ID | View Type | Content / Action |
|---|---|---|
| `R.id.notification_title` | `TextView` | "Recording" / "Summarizing..." |
| `R.id.notification_chronometer` | `Chronometer` | Base set to elapsed realtime at session start |
| `R.id.notification_saved_count` | `TextView` | Formatted string (e.g., `3 saved`) |
| `R.id.btn_notification_rewind` | `ImageButton` | Set `setOnClickPendingIntent` to `ACTION_REWIND` |
| `R.id.btn_notification_stop` | `ImageButton` | Set `setOnClickPendingIntent` to `ACTION_STOP` |
