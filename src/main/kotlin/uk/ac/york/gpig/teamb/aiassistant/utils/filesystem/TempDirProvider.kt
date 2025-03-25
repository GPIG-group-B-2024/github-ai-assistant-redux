package uk.ac.york.gpig.teamb.aiassistant.utils.filesystem

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

/**
 * Create a temporary directory, use it for some stuff, possibly returning a value, then destroy it.
 * */
@OptIn(ExperimentalPathApi::class)
fun <T> withTempDir(block: (Path) -> T): T {
    val tempDir = createTempDirectory()
    return block(tempDir).also { tempDir.toFile().deleteRecursively() }
}
