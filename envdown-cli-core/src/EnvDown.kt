package dev.fnc314.tools.envdown.cli.core

private val unescapePattern = Regex("\\\\\\$\\{env\\.")

public fun processEnvDown(
    input: String, 
    strictMode: Boolean = false,
    getEnv: (String) -> String?
): String {
    val patternWithDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\:-(.*?)\\}")
    val patternWithoutDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\}")

    val processedText = patternWithDefault.replace(input) { matchResult ->
        val varName = matchResult.groups[1]?.value ?: ""
        val defaultValue = matchResult.groups[2]?.value ?: ""
        val envValue = getEnv(varName)

        when {
            !envValue.isNullOrEmpty() -> envValue
            else -> defaultValue
        }
    }

    val processedText2 = patternWithoutDefault.replace(processedText) { matchResult ->
        val varName = matchResult.groups[1]?.value ?: ""
        val envValue = getEnv(varName)

        when {
            !envValue.isNullOrEmpty() -> envValue
            strictMode -> throw IllegalArgumentException(
                "EnvDown Error: Environment variable '$varName' is missing and no default was provided."
            )
            else -> ""
        }
    }

    return processedText2.replace(unescapePattern, """\$\{env.""")
}
