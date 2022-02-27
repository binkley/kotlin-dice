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
            it.reroll shouldBe 2
            it.keep shouldBe 3
            it.explode shouldBe 4
            it.multiply shouldBe 2
        }

        val roller = Roller(
            DiceExpression(
                dieSides = 6,
                dieBase = ONE,
                diceCount = 7,
                reroll = 2,
                keep = 3,
                explode = 4,
                multiply = 2,
            ),
            random = stableSeedForEachTest(),
            reporting = assertOnRoll
        )
        roller.rollDice()
    }
}
