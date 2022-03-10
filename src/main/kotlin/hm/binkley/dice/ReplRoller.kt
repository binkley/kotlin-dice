package hm.binkley.dice

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
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.Path
import kotlin.random.Random

private val HISTORY_PATH = pathInHome(".roll_history")

/**
 * Creates a [Terminal] and [LineReaderBuilder] pair.
 * The terminal is ready to use.
 * The line reader is returned as a builder for further customization with
 * commands, completion, etc.
 */
fun interface NewRepl {
    operator fun invoke(options: Options): Pair<Terminal, LineReaderBuilder>
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
        this.lineReader = lineReader.build()
    }

    /** Note: closes (and resets) the terminal when done. */
    override fun rollAndReport() = terminal.use {
        while (true) try {
            // TODO: Untested, and @Generated does compile for lambdas
            rollFromLines { lineReader.readLine(options.prompt) }
        } catch (e: DiceException) {
            options.commandLine.err.println(colorScheme.errorText(e.message))
        }
    }
}

val newRealRepl = NewRepl { options ->
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()

    val lineReaderBuilder = LineReaderBuilder.builder()
        .terminal(terminal)
    if (options.history) lineReaderBuilder
        .variable(HISTORY_FILE, HISTORY_PATH)

    terminal to lineReaderBuilder
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
val newTestRepl = NewRepl {
    val terminal = DumbTerminal(
        PROGRAM_NAME,
        if (inColor()) TYPE_DUMB_COLOR else TYPE_DUMB,
        System.`in`,
        System.out,
        UTF_8,
    )

    // No history during testing
    val lineReaderBuilder = LineReaderBuilder.builder()
        .terminal(terminal)
        .variable(HISTORY_SIZE, 0)

    terminal to lineReaderBuilder
}

@Suppress("SameParameterValue")
private fun pathInHome(fileName: String) =
    Path(System.getProperty("user.home"), fileName)

private fun inColor() = AUTO.enabled()
