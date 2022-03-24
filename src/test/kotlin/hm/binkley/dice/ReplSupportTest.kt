package hm.binkley.dice

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ReplSupportTest {
    @Test
    fun `should GNU prefix`() {
        null.maybeGnuPrefix() shouldBe "roll: "
        "Cool".maybeGnuPrefix() shouldBe "roll: Cool"

        runWithEofConsole {
            null.maybeGnuPrefix() shouldBe ""
            "Cool".maybeGnuPrefix() shouldBe "Cool"
        }
    }
}
