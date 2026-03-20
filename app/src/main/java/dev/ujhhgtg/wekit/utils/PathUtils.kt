package dev.ujhhgtg.wekit.utils

import java.nio.file.Path
import kotlin.io.path.createDirectories

fun Path.createDirectoriesNoThrow(): Path {
    runCatching { this.createDirectories() }
    return this
}
