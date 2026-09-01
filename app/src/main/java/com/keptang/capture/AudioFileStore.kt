package com.keptang.capture

import android.content.Context
import java.io.File

/** Manages the app-private directory that holds raw capture audio. */
class AudioFileStore(context: Context) {

    private val directory: File = File(context.filesDir, "captures").apply { mkdirs() }

    fun newFileFor(captureId: String): File = File(directory, "$captureId.wav")

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }
}
