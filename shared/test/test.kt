
import com.fnc314.tools.envdown.processEnvDown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EnvDownTest {
    @Test
    fun testStandardInterpolation() {
        val result = processEnvDown("Hello \${env.USER:-World}")
        assertTrue(result.startsWith("Hello "))
    }

    @Test
    fun testDefaultValues() {
        val input = "Server: \${env.MY_NONEXISTENT_VAR_12345:-localhost}"
        val result = processEnvDown(input)
        assertEquals("Server: localhost", result)
    }

    @Test
    fun testMissingVariableLenientMode() {
        val input = "Key: \${env.MY_NONEXISTENT_VAR_12345}"
        val result = processEnvDown(input, strictMode = false)
        assertEquals("Key: ", result)
    }

    @Test
    fun testMissingVariableStrictMode() {
        val input = "Key: \${env.MY_NONEXISTENT_VAR_12345}"
        assertFailsWith<IllegalArgumentException> {
            processEnvDown(input, strictMode = true)
        }
    }

    @Test
    fun testEscapedVariable() {
        val input = "Literal: \\\${env.MY_NONEXISTENT_VAR_12345}"
        val result = processEnvDown(input)
        assertEquals("Literal: \${env.MY_NONEXISTENT_VAR_12345}", result)
    }

    @Test
    fun testEscapedVariableWithDefault() {
        val input = "Literal: \\\${env.MY_NONEXISTENT_VAR_12345:-fallback}"
        val result = processEnvDown(input)
        assertEquals("Literal: \${env.MY_NONEXISTENT_VAR_12345:-fallback}", result)
    }

    @Test
    fun testMultipleVariables() {
        val input = "A: \${env.MISSING_A:-a}, B: \${env.MISSING_B:-b}"
        val result = processEnvDown(input)
        assertEquals("A: a, B: b", result)
    }

    @Test
    fun testVariableWithUnderscore() {
        val input = "Val: \${env._MY_VAR_123:-under_score}"
        val result = processEnvDown(input)
        assertEquals("Val: under_score", result)
    }

    @Test
    fun testEmptyInput() {
        val result = processEnvDown("")
        assertEquals("", result)
    }

    @Test
    fun testNoVariables() {
        val input = "Just plain text"
        val result = processEnvDown(input)
        assertEquals("Just plain text", result)
    }

    @Test
    fun testMixedContent() {
        val input = "# Title\nServer: \${env.SERVER:-localhost}\nPort: \${env.PORT:-8080}\nEscaped: \\\${env.SECRET}"
        val result = processEnvDown(input)
        assertTrue(result.contains("Server: localhost"))
        assertTrue(result.contains("Port: 8080"))
        assertTrue(result.contains("Escaped: \${env.SECRET}"))
    }
}