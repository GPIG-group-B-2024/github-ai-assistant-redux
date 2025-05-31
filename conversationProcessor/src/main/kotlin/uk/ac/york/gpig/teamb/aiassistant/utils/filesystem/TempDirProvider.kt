package uk.ac.york.gpig.teamb.aiassistant.utils.filesystem

import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/**
 * Create a temporary directory, use it for some stuff, possibly returning a value, then destroy it.
 */
fun <T> withTempDir(block: (Path) -> T): T {
    val tempDir = createTempDirectory()
    return block(tempDir).also { tempDir.toFile().deleteRecursively() }
}
