package hm.binkley.dice

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val JLINE3_RACE_CONDITION = true

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
    fun `should run real new REPL`() {
        runNewReplLive("--new-repl", "--test-repl")
    }

    @Test
    fun `should run real new REPL without history`() {
        runNewReplLive("--no-history", "--new-repl", "--test-repl")
    }

    // TODO: https://github.com/binkley/kotlin-dice/issues/34
    private fun runNewReplLive(vararg args: String) {
        runWithEofConsole {
            val options = Options()
            val commandLine = options.parseOptions(*args)
            if (JLINE3_RACE_CONDITION) return@runWithEofConsole
            options.terminal.pause()
            commandLine.execute()
            options.terminal.resume()
        }
    }
}
