package dev.fnc314.tools.envdown.cli.core

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.system.exitProcess

fun runCli(args: Array<String>, getEnv: (String) -> String?) {
    var strictMode = false
    var filePath: String? = null

    for (arg in args) {
        if (arg == "--strict") {
            strictMode = true
        } else if (!arg.startsWith("-")) {
            filePath = arg
        }
    }

    if (filePath == null) {
        println("Usage: envdown-cli [--strict] <file.envdown>")
        exitProcess(1)
    }

    val path = Path(filePath)
    if (!SystemFileSystem.exists(path)) {
        println("Error: File '$filePath' not found.")
        exitProcess(1)
    }

    try {
        val input = SystemFileSystem.source(path).buffered().readString()
        val output = processEnvDown(input, strictMode, getEnv)
        println(output)
    } catch (e: Exception) {
        println(e.message)
        exitProcess(1)
    }
}
