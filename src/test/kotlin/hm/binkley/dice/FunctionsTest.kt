package hm.binkley.dice

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

internal class FunctionsTest {
    @Test
    fun `should be non-interactive in tests`() {
        isInteractive().shouldBeFalse()
    }

    @Test
    fun `should be interactive in test mocks when required`() {
        runWithEofConsole {
            isInteractive().shouldBeTrue()
        }
    }
}
