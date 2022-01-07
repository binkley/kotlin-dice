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

                if (null == expected) result.parseErrors.shouldNotBeEmpty()
                else result.parseErrors.shouldBeEmpty()
            }
    }
}
