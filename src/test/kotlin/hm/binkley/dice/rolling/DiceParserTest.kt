package hm.binkley.dice.rolling

import hm.binkley.dice.demoExpressions
import hm.binkley.dice.rolling.DiceParser.Companion.dice
import hm.binkley.dice.stableSeedForTesting
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DiceParserTest {
    @Test
    fun `should parse and roll`() {
        for ((expression, expected, description) in demoExpressions)
            withClue("$expression ($description)") {
                // Recreate each time to reset the seed each time
                val dice = dice(stableSeedForTesting())
                try {
                    val result = dice.roll(expression)

                    result.resultValue shouldBe expected

                    when (expected) {
                        null -> result.parseErrors.shouldNotBeEmpty()
                        else -> result.parseErrors.shouldBeEmpty()
                    }
                } catch (e: DiceException) {
                    // Case when app exception thrown from within parsing as
                    // part of validation for business rules
                    expected.shouldBeNull()
                }
            }
    }

    @Test
    fun `should use default RNG`() {
        val dice = dice()
        val result = dice.roll("1d1")

        result.resultValue shouldBe 1
        result.parseErrors.shouldBeEmpty()
    }

    @Test
    fun `should reuse existing dice parser`() {
        val dice = dice(stableSeedForTesting())
        // Complex expression to show state is reset
        dice.roll("100d3r1h99!+100d3r1l99!3-17")
        val result = dice.roll("d6")

        result.resultValue shouldBe 4
        result.parseErrors.shouldBeEmpty()
    }
}
