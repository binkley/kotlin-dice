package hm.binkley.dice

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.parboiled.Parboiled.createParser
import org.parboiled.parserunners.ReportingParseRunner
import java.util.Arrays
import java.util.stream.Stream

internal class ParserTest {
    @MethodSource("args")
    @ParameterizedTest
    fun `should parse`(expression: String, expected: Int?) {
        val random = stableSeedForEachTest()

        val result = ReportingParseRunner<Int>(
            createParser(
                DiceParser::class.java,
                random
            ).diceExpression()
        ).run(expression)

        result.resultValue shouldBe expected
        if (null == expected)
            result.parseErrors.shouldNotBeEmpty()
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun args(): Stream<Arguments> {
            return Arrays.stream(demoExpressions)
                .map { (expression, result) ->
                    Arguments.of(expression, result)
                }
        }
    }
}
