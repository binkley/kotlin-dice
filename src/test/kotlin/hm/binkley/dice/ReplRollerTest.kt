package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.matchers.booleans.shouldBeTrue
import org.jline.reader.EndOfFileException
import org.jline.terminal.impl.AbstractTerminal
import org.junit.jupiter.api.Test

internal class ReplRollerTest {
    @Test
    fun `should close terminal when done`() {
        val options = Options()

        var closed = false

        withTextFromSystemIn().execute {
            tapSystemErrAndOut {
                options.parseOptions("--test-repl")
                (options.terminal as AbstractTerminal).setOnClose {
                    closed = true
                }
                try {
                    options.pickReplRoller(
                        stableSeedForTesting(),
                        MainReporter.new(options.minimum, options.verbose)
                    ).rollAndReport()
                } catch (ignored: EndOfFileException) {
                }
            }
        }

        closed.shouldBeTrue()
    }
}
