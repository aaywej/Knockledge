package group.two.knockledge.core

import java.io.File

/** Cache directory holding the rewound audio chunks, created on first use. */
fun audioBufferDir(cacheDir: File): File = File(cacheDir, "audio_buffer").also { it.mkdirs() }
