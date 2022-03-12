package hm.binkley.dice

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

internal class ReplSupportTest {
    @Test
    fun `should GNU prefix`() {
        null.maybeGnuPrefix() shouldBe "roll: "
        "Cool".maybeGnuPrefix() shouldBe "roll: Cool"

        // TODO: Better than string name of JVM Kt class!
        mockkStatic("hm.binkley.dice.ReplSupportKt") {
            every { isInteractive() } returns true
            null.maybeGnuPrefix() shouldBe ""
            "Cool".maybeGnuPrefix() shouldBe "Cool"
        }
    }
}
