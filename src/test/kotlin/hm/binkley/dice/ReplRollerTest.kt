package hm.binkley.dice

import io.kotest.matchers.booleans.shouldBeTrue
import org.jline.reader.EndOfFileException
import org.junit.jupiter.api.Test
import kotlin.system.exitProcess

internal class ReplRollerTest {
    @Test
    fun `should close terminal when done`() {
        var closed = false

        captureWithInput {
            val options = Options()
            // Parse first so `terminal` exists (lateinit)
            options.parseOptions("--test-repl")
            options.terminal.setOnClose {
                closed = true
            }
            try {
                options.pickReplRoller(
                    stableSeedForTesting(),
                    MainReporter.new(options.minimum, options.verbose)
                ).rollAndReport()
            } catch (ignored: EndOfFileException) {
            }
            exitProcess(0) // Needed by system-lambda stubbing
        }

        closed.shouldBeTrue()
    }
}
