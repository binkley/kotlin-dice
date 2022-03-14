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
        val options = Options()
        val (commandLine, terminal) = options.commandLineAndTerminal(
            "--new-repl"
        )
        commandLine.installNewRepl(options, terminal)
    }

    @Test
    fun `should create new real REPL without history`() {
        val options = Options()
        val (commandLine, terminal) = options.commandLineAndTerminal(
            "--no-history", "--new-repl"
        )
        commandLine.installNewRepl(options, terminal)
    }
}
