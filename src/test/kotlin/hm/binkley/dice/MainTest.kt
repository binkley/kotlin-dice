package hm.binkley.dice

import org.junit.jupiter.api.Test
import org.parboiled.Parboiled
import org.parboiled.parserunners.ReportingParseRunner

internal class MainTest {
    @Test
    fun `should not throw exception on roll`() {
        roll("3d6")
    }

    @Test
    fun `should default construct with the RNG`() {
        ReportingParseRunner<Int>(
            Parboiled.createParser(DiceParser::class.java).diceExpression()
        ).run("3d6")
    }
}
