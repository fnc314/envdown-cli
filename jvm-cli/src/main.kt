import com.fnc314.tools.envdown.processEnvDown
import java.io.File
import kotlin.system.exitProcess

/**
 * JVM CLI entry point for EnvDown.
 *
 * Usage: envdown [--strict] <file.envdown>
 */
fun main(args: Array<String>): Unit {
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
        println("Usage: envdown [--strict] <file.envdown>")
        exitProcess(1)
    }

    val file = File(filePath)
    if (!file.exists()) {
        println("Error: File '$filePath' not found.")
        exitProcess(1)
    }

    try {
        val input = file.readText()
        val output = processEnvDown(input, strictMode)
        println(output)
    } catch (e: Exception) {
        println(e.message ?: "Error processing file.")
        exitProcess(1)
    }
}
