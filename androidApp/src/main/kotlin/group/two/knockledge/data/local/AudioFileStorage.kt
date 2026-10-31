package group.two.knockledge.data.local

import android.content.Context
import java.io.File

/** Where saved note audio lives inside the app's private storage. */
object AudioFileStorage {
    /** Returns the note audio directory, creating it if missing. */
    fun audioDir(context: Context): File = File(context.filesDir, "audio").apply { mkdirs() }
}
