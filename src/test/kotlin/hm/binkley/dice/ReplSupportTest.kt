package hm.binkley.dice

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test
import java.io.Console
import java.lang.System.console

internal class ReplSupportTest {
    @Test
    fun `should GNU prefix`() {
        null.maybeGnuPrefix() shouldBe "roll: "
        "Cool".maybeGnuPrefix() shouldBe "roll: Cool"

        val interactiveConsole = mockk<Console>()
        mockkStatic(System::class) {
            every { console() } returns interactiveConsole
            null.maybeGnuPrefix() shouldBe ""
            "Cool".maybeGnuPrefix() shouldBe "Cool"
        }
    }
    
    @Test
    fun `should create old real REPL`() {
        val options = Options() // all defaults

        options.oldLineReader(realTerminal())
    }

    @Test
    fun `should create old real REPL without history`() {
        val options = Options()
        options.history = false

        options.oldLineReader(realTerminal())
    }

    @Test
    fun `should create new real REPL`() {
        Options().parseOptions(
            "--new-repl"
        )
    }

    @Test
    fun `should create new real REPL without history`() {
        Options().parseOptions(
            "--no-history", "--new-repl"
        )
    }
}
