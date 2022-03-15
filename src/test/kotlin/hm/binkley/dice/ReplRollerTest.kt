package hm.binkley.dice

import org.junit.jupiter.api.Test

internal class ReplRollerTest {
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
