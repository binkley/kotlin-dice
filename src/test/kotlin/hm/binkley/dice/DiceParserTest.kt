package hm.binkley.dice

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.parboiled.Parboiled.createParser
import org.parboiled.parserunners.ReportingParseRunner

internal class ParserTest {
    @Test
    fun `should parse and roll`() {
        for ((expression, expected) in demoExpressions)
            withClue(expression) {
                val result = runner().run(expression)

                result.resultValue shouldBe expected

                if (null != expected) result.parseErrors.shouldBeEmpty()
                else result.parseErrors.shouldNotBeEmpty()
            }
    }
}

/** Creates a new runner each time so the `Random` is reset each time. */
private fun runner() = ReportingParseRunner<Int>(
    createParser(
        DiceParser::class.java,
        stableSeedForEachTest()
    ).diceExpression()
)
