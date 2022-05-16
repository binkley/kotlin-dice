package hm.binkley.dice.rolling

import hm.binkley.dice.rolling.DieBase.ONE
import hm.binkley.dice.stableSeedForTesting
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RollerTest {
    @Test
    fun `should report roll details`() {
        var assertionExecuted = false
        val assertOnRoll = RollReporter {
            assertionExecuted = true
            it.expression shouldBe "7d6r2h3!4x2"
            it.dieSides shouldBe 6
            it.dieBase shouldBe ONE
            it.diceCount shouldBe 7
            it.rerollLow shouldBe 2
            it.keepCount shouldBe KeepHigh(3)
            it.explodeHigh shouldBe 4
            it.multiply shouldBe 2
        }

        Roller(
            stableSeedForTesting(),
            assertOnRoll,
            ParsedDice(
                expression = "7d6r2h3!4x2",
                dieBase = ONE,
                dieSides = 6,
                diceCount = 7,
                rerollLow = 2,
                keepCount = KeepHigh(3),
                explodeHigh = 4,
                multiply = 2,
            )
        ).rollDice()

        assertionExecuted.shouldBeTrue()
    }
}
