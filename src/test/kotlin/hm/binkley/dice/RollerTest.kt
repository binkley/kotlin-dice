package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RollerTest {
    @Test
    fun `should report roll details`() {
        val assertOnRoll = RollReporter {
            it.dieSides shouldBe 6
            it.dieBase shouldBe ONE
            it.diceCount shouldBe 7
            it.rerollLow shouldBe 2
            it.keepCount shouldBe 3
            it.explodeHigh shouldBe 4
            it.multiply shouldBe 2
        }

        val roller = Roller(
            random = stableSeedForEachTest(),
            reporting = assertOnRoll,
            ParsedDice(
                dieSides = 6,
                dieBase = ONE,
                diceCount = 7,
                rerollLow = 2,
                keepCount = 3,
                explodeHigh = 4,
                multiply = 2,
            )
        )
        roller.rollDice()
    }
}
