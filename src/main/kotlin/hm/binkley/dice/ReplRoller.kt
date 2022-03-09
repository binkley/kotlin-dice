package hm.binkley.dice

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReader.HISTORY_FILE
import org.jline.reader.LineReader.HISTORY_SIZE
import org.jline.reader.LineReaderBuilder
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

private val HISTORY_PATH = pathInHome(".roll_history")

fun interface NewRepl {
    operator fun invoke(options: Options): Pair<Terminal, LineReader>
}

class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
    newRepl: NewRepl,
) : MainRoller(random, reporter) {
    private val terminal: Terminal
    private val lineReader: LineReader

    init {
        val (terminal, lineReader) = newRepl(options)
        this.terminal = terminal
        this.lineReader = lineReader
    }

    /** Note: closes (and resets) the terminal when done. */
    override fun rollAndReport() = terminal.use {
        while (true) try {
            // TODO: Untested, and @Generated does compile for lambdas
            rollFromLines { lineReader.readLine(options.prompt) }
        } catch (e: DiceException) {
            err.println(colorScheme.errorText(e.message))
        } catch (e: EndOfFileException) {
            return
        }
    }
}

val newRealRepl = NewRepl { options ->
    val terminalBuilder = TerminalBuilder.builder()
        .name(PROGRAM_NAME)

    val lineReaderBuilder = LineReaderBuilder.builder()
        .terminal(terminalBuilder.build())
    if (options.history) lineReaderBuilder
        .variable(HISTORY_FILE, HISTORY_PATH)

    terminalBuilder.build() to lineReaderBuilder.build()
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
val newTestRepl = NewRepl {
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
        .variable(HISTORY_SIZE, 0)
        .build()
    terminal to lineReader
}

@Suppress("SameParameterValue")
private fun pathInHome(fileName: String) =
    Path(System.getProperty("user.home"), fileName)
