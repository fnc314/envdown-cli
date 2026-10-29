package com.fnc314.tools.envdown

/**
 * Core EnvDown processor.
 *
 * EnvDown is a pre-processor that runs before Markdown parsing.
 * It replaces `${env.VAR_NAME}` tokens with values from the host environment,
 * supports default values via `${env.VAR_NAME:-default}`, and allows escaping
 * via `\${env.VAR_NAME}`.
 */

/**
 * Retrieves an environment variable by key.
 * Platform-specific implementation via expect/actual.
 */
public expect fun getEnv(key: String): String?

private val unescapePattern = Regex("\\\\\\$\\{env\\.")

/**
 * Processes raw EnvDown text and outputs standard Markdown with all interpolations resolved.
 *
 * @param input The raw EnvDown text containing `${env.VAR}` tokens.
 * @param strictMode If true, throws an error when a variable is missing and has no default.
 *   If false, missing variables resolve to an empty string.
 * @return The processed text with all interpolations replaced.
 * @throws IllegalArgumentException if strictMode is true and a required variable is missing.
 */
public fun processEnvDown(input: String, strictMode: Boolean = false): String {
    // Matches ${env.VAR} and ${env.VAR:-default} using negative lookbehind for escaping
    // Use two separate patterns to avoid Kotlin's named group parsing issues with (?::
    val patternWithDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\:-(.*?)\\}")
    val patternWithoutDefault = Regex("(?<!\\\\)\\$\\{env\\.([a-zA-Z_][a-zA-Z0-9_]*)\\}")

    // First replace variables with defaults, then variables without defaults
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

    // Unescape literal tokens: \${env.VAR} -> ${env.VAR}
    // Escape $ for both Kotlin templates and Java regex replacement
    return processedText2.replace(unescapePattern, """\$\{env.""")
}
