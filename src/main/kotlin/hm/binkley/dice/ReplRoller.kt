package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Attributes
import org.jline.terminal.Attributes.InputFlag.IGNCR
import org.jline.terminal.Attributes.LocalFlag.ECHO
import org.jline.terminal.Attributes.OutputFlag.OPOST
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import picocli.CommandLine.Help.Ansi.AUTO
import java.lang.System.err
import java.nio.charset.StandardCharsets.UTF_8
import java.util.EnumSet
import kotlin.random.Random

class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val prompt: String,
    newReplReader: () -> Pair<Terminal, LineReader>,
) : MainRoller(random, reporter) {
    private val terminal: Terminal
    private val replReader: LineReader

    init {
        val (terminal, replReader) = newReplReader()
        this.terminal = terminal
        this.replReader = replReader
    }

    override fun rollAndReport() {
        terminal.use { // Terminals need closing to reset the external terminal
            try {
                while (true) try {
                    // TODO: Untested and @Generated does compile for lambdas
                    rollFromLines { replReader.readLine(prompt) }
                } catch (e: DiceException) {
                    err.println(colorScheme.errorText(e.message))
                }
            } catch (e: UserInterruptException) {
                throw e // Let main() handle this for the right exit code
            } catch (e: EndOfFileException) {
                return
            }
        }
    }
}

@Generated // Lie to JaCoCo -- the real terminal blocks on read in tests
fun replReader(): Pair<Terminal, LineReader> {
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()

    val replReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    return terminal to replReader
}

fun testReplReader(): Pair<DumbTerminal, LineReader> {
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
