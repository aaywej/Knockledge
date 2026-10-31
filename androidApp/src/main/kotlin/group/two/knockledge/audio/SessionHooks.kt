package group.two.knockledge.audio

/**
 * Lets each platform show its "Knockledge in progress" surface:
 * Android starts a microphone foreground service with an ongoing notification,
 * iOS will start a Live Activity (implement this interface in Swift).
 */
interface SessionHooks {
    /** Called when recording starts; show the ongoing notification or Live Activity. */
    fun onSessionStarted()
    /** Called when recording stops; remove that surface. */
    fun onSessionStopped()
}

/** Does nothing, for platforms or tests that need no session surface. */
object NoopSessionHooks : SessionHooks {
    /** No surface to show; does nothing. */
    override fun onSessionStarted() = Unit
    /** No surface to remove; does nothing. */
    override fun onSessionStopped() = Unit
}

