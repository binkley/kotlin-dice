package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import picocli.CommandLine.Help.Ansi.AUTO
import java.lang.System.err
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.Path
import kotlin.random.Random

class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val prompt: String,
    newRepl: () -> Pair<Terminal, LineReader>,
) : MainRoller(random, reporter) {
    private val terminal: Terminal
    private val lineReader: LineReader

    init {
        val (terminal, lineReader) = newRepl()
        this.terminal = terminal
        this.lineReader = lineReader
    }

    override fun rollAndReport() {
        terminal.use { // Terminals need closing to reset the external terminal
            try {
                while (true) try {
                    // TODO: Untested, and @Generated does compile for lambdas
                    rollFromLines { lineReader.readLine(prompt) }
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
fun newRepl(): Pair<Terminal, LineReader> {
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()
    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .history(DefaultHistory())
        .variable(
            LineReader.HISTORY_FILE,
            Path(System.getProperty("user.home"), ".roll_history")
        )
        .build()
    return terminal to lineReader
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
fun newTestRepl(): Pair<Terminal, LineReader> {
    val terminal = DumbTerminal(
        PROGRAM_NAME,
        if (AUTO.enabled()) TYPE_DUMB_COLOR else TYPE_DUMB,
        System.`in`,
        System.out,
        UTF_8,
    )
    // No history during testing
    val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    return terminal to lineReader
}
