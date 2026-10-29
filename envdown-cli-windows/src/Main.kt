package dev.fnc314.tools.envdown.cli.windows

import dev.fnc314.tools.envdown.cli.core.runCli
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    runCli(args) { key -> getenv(key)?.toKString() }
}
