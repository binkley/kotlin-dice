package hm.binkley.dice

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized
import com.github.stefanbirkner.systemlambda.SystemLambda.withTextFromSystemIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Attributes
import org.jline.terminal.Attributes.InputFlag.IGNCR
import org.jline.terminal.Attributes.LocalFlag.ECHO
import org.jline.terminal.Attributes.OutputFlag.OPOST
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.impl.DumbTerminal
import org.junit.jupiter.api.Test
import picocli.CommandLine.Help.Ansi.AUTO
import java.nio.charset.StandardCharsets.UTF_8
import java.util.EnumSet
import kotlin.random.Random

internal class ReplRollerTest {
    @Test
    fun `should XXX`() {
        fun readAndWrite() {
            val roller = ReplRoller(
                Random(1),
                MainReporter.new(Int.MIN_VALUE, false),
                "", // This test does not test the prompt
                ::testRepl,
            )

            roller.rollAndReport()
        }

        var stdout = "BUG in test method"
        withTextFromSystemIn("1d1").execute {
            val stderr = tapSystemErrNormalized {
                stdout = tapSystemOutNormalized {
                    readAndWrite()
                }
            }

            stdout shouldBe "1d1 1\n"
            stderr.shouldBeEmpty()
        }
    }
}

fun testRepl(): Pair<DumbTerminal, LineReader> {
    val terminal = DumbTerminal(
        PROGRAM_NAME,
        if (AUTO.enabled()) TYPE_DUMB_COLOR else TYPE_DUMB,
        System.`in`,
        System.out,
        UTF_8,
    )

    val attributes: Attributes = terminal.attributes
    attributes.setLocalFlag(ECHO, true)
    attributes.setInputFlag(IGNCR, true)
    attributes.outputFlags = EnumSet.of(OPOST)
    terminal.attributes = attributes

    val replReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()

    return terminal to replReader
}
