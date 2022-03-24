package hm.binkley.dice

import io.kotest.matchers.shouldBe
import org.jline.terminal.Terminal
import org.jline.terminal.impl.PosixPtyTerminal
import org.junit.jupiter.api.Test
import java.io.FileInputStream

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
            val terminal = options.terminal
            terminal.pause()
            commandLine.execute()
            terminal.maybeResume()
        }
    }
}

/**
 * @todo Strongly a hack to work around JLine3 raciness with Pty stream
 *       handling.
 */
private fun Terminal.maybeResume() {
    if (this !is PosixPtyTerminal) {
        System.err.println(
            "@|bold,red WARNING: NOT A POSIX PTY TERMINAL|@".colored
        )
        return
    }
    // Assume `masterInput` is a `FileInputStream`: true for the JNA and
    // JANSI implementations (the Exec implementation raises unimplemented).
    // Assume invalid STDIN means closed and does not mean currupted
    if (!(pty.masterInput as FileInputStream).fd.valid()) return

    resume()
}
