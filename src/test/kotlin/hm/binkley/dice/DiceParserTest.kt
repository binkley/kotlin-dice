package hm.binkley.dice

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.parboiled.Parboiled.createParser
import org.parboiled.parserunners.ReportingParseRunner
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
        fun args(): Stream<Arguments> = Stream.of(
            Arguments.of("D6", 4),
            Arguments.of("z6", 3),
            Arguments.of("Z6", 3),
            Arguments.of("3d6", 10),
            Arguments.of("3D6", 10),
            Arguments.of("3d6+1", 11),
            Arguments.of("3d6-1", 9),
            Arguments.of("10d3!", 20),
            Arguments.of("10d3!2", 49),
            Arguments.of("4d6h3", 10),
            Arguments.of("4d6H3", 10),
            Arguments.of("4d6l3", 6),
            Arguments.of("4d6L3", 6),
            Arguments.of("3d6+2d4", 17),
            Arguments.of("d%", 66),
            Arguments.of("6d4l5!", 20),
            Arguments.of("3d3r1h2!", 10),
            Arguments.of("3d3R1h2!", 10),
            Arguments.of("100d3r1h99!+100d3r1l99!3-17", 919),
            Arguments.of("blah", null),
        )
    }
}
