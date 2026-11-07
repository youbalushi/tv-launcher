package om.yousef.shashati.data

import android.content.Context
import java.io.File

/**
 * Adhkar recordings the owner copies to the TV from the laptop (never published online).
 * They live in /sdcard/Android/data/om.yousef.shashati/files/adhkar as morning.* and evening.*
 */
object AdhkarAudio {
    fun dir(context: Context): File? = context.getExternalFilesDir("adhkar")?.apply { mkdirs() }

    fun file(context: Context, morning: Boolean): File? {
        val name = if (morning) "morning" else "evening"
        return dir(context)?.listFiles()?.firstOrNull { it.nameWithoutExtension == name && it.length() > 10_000 }
    }
}
