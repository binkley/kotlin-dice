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

    @Test
    fun `should create old real REPL`() {
        Options().parseOptions()
    }

    @Test
    fun `should create old real REPL without history`() {
        Options().parseOptions(
            "--no-history"
        )
    }

    @Test
    fun `should create new real REPL`() {
        runWithEofConsole {
            Options().parseOptions(
                "--new-repl"
            )
        }
    }

    @Test
    fun `should create new real REPL without history`() {
        runWithEofConsole {
            Options().parseOptions(
                "--no-history", "--new-repl"
            )
        }
    }
}
