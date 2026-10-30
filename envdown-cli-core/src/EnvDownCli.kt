package dev.fnc314.tools.envdown.cli.core

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.system.exitProcess

fun runCli(args: Array<String>, getEnv: (String) -> String?) {
    var strictMode = false
    var filePath: String? = null
    var outputFile: String? = null

    args.forEachIndexed { indx, arg ->
        when (arg) {
            "--strict" -> {
                strictMode = true
            }
            "--output", "-o" -> {
                outputFile = args[indx + 1]
            }
            else -> {
                if (!arg.startsWith("-")) {
                    filePath = arg
                }
            }
        }
    }

    if (filePath == null) {
        println("Usage: envdown-cli [--strict] [--output|-o <file.md>] <file.envdown>")
        exitProcess(1)
    }

    val path = Path(filePath)
    if (!SystemFileSystem.exists(path)) {
        println("Error: File '$filePath' not found.")
        exitProcess(2)
    }

    try {
        val input = SystemFileSystem.source(path).buffered().readString()
        val output = processEnvDown(input, strictMode, getEnv)
        if (outputFile != null) {
            output.writeToFile(outputFile = outputFile)
        } else {
            println(output)
        }
    } catch (e: Exception) {
        println(e.message)
        exitProcess(255)
    }
}

private fun String.writeToFile(outputFile: String) {
    if (!outputFile.endsWith("md")) {
        println("Error: output file is not .md")
        exitProcess(3)
    }
    val outputPathPath = Path(outputFile).also {
        if (SystemFileSystem.exists(it)) {
            SystemFileSystem.delete(it, true)
        }
        if (it.parent != null) {
            SystemFileSystem.createDirectories(
                it.parent!!,
            )
        }
    }
    SystemFileSystem
        .sink(
            outputPathPath,
            true
        )
        .buffered()
        .use { sink ->
            sink.write(
                encodeToByteArray(),
                0,
                length
            )
        }
}
