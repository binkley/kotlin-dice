package hm.binkley.dice

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DiceParserTest {
    @Test
    fun `should parse and roll`() {
        for ((expression, expected) in demoExpressions)
            withClue(expression) {
                val result = roll(
                    expression = expression,
                    random = stableSeedForEachTest(),
                    reporter = silentTestingReporter
                )

                result.resultValue shouldBe expected

                when (expected) {
                    null -> result.parseErrors.shouldNotBeEmpty()
                    else -> result.parseErrors.shouldBeEmpty()
                }
            }
    }

    @Test
    fun `should use default RNG`() {
        val result = roll("1d1") { }

        result.resultValue shouldBe 1
        result.parseErrors.shouldBeEmpty()
    }
}
