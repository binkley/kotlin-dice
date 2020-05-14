package hm.binkley.dice

import ch.tutteli.atrium.api.fluent.en_GB.isNotEmpty
import ch.tutteli.atrium.api.fluent.en_GB.toBe
import ch.tutteli.atrium.api.verbs.expect
import java.util.stream.Stream
import kotlin.random.Random
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.parboiled.Parboiled.createParser
import org.parboiled.parserunners.ReportingParseRunner

private fun stableSeedForEachTest() = Random(1L)

@TestInstance(PER_CLASS)
internal class StandAloneTest {
    @Test
    fun `should not throw on roll`() {
        roll("3d6")
    }

    @Test
    fun `should default construct`() {
        ReportingParseRunner<Int>(
            createParser(DiceParser::class.java).diceExpression()
        ).run("100d3r1h99!+100d3r1l99!3-17")
    }
}

@TestInstance(PER_CLASS)
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

        expect(result.resultValue).toBe(expected)
        if (null == expected)
            expect(result.parseErrors).isNotEmpty()
    }

    internal companion object {
        @JvmStatic
        @Suppress("unused")
        fun args(): Stream<Arguments> = Stream.of(
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
            Arguments.of("blah", null),
            Arguments.of("d6", 4)
        )
    }
}
