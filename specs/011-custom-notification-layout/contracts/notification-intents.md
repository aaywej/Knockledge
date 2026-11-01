# Interface Contract: Notification Actions & Service Intents

## Service Intents (`KnockledgeService`)

The custom notification layout binds user tap events directly to PendingIntents targeting `KnockledgeService`.

### 1. Rewind Action (`ACTION_REWIND`)
- **Action String**: `group.two.knockledge.action.REWIND`
- **Sender**: `RemoteViews.setOnClickPendingIntent(R.id.btn_notification_rewind, pendingIntent)`
- **PendingIntent Type**: `PendingIntent.getService(context, 1, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`
- **Service Handler**: Calls `controller.rewind(TriggerType.MANUAL)`
- **Side Effects**: Saves current circular audio buffer clip, increments `savedCount` by 1, posts updated notification state.

---

### 2. Stop Action (`ACTION_STOP`)
- **Action String**: `group.two.knockledge.action.STOP`
- **Sender**: `RemoteViews.setOnClickPendingIntent(R.id.btn_notification_stop, pendingIntent)`
- **PendingIntent Type**: `PendingIntent.getService(context, 2, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`
- **Service Handler**: Calls `controller.stop()`
- **Side Effects**: Stops audio recorder, releases wake locks/foreground service status (`stopForeground(STOP_FOREGROUND_REMOVE)`), terminates `KnockledgeService`.

---

### 3. Open App Action (Content Intent)
- **Target Activity**: `group.two.knockledge.MainActivity`
- **Sender**: `builder.setContentIntent(pendingIntent)`
- **PendingIntent Type**: `PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`
- **Flags**: `Intent.FLAG_ACTIVITY_SINGLE_TOP`
- **Side Effects**: Launches or brings existing app activity instance to front without re-creating stack.
