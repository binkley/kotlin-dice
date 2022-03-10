package hm.binkley.dice

import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReader.HISTORY_FILE
import org.jline.reader.LineReader.HISTORY_SIZE
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultExpander
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import picocli.CommandLine.Help.Ansi.AUTO
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.random.Random

private const val HISTORY_FILE_NAME = ".roll_history"

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
        val (terminal, lineReaderBuilder) = newRepl(options)
        this.terminal = terminal
        lineReader = lineReaderBuilder.build()
    }

    /** Note: closes (and resets) the terminal when done. */
    override fun rollAndReport() = terminal.use {
        while (true) try {
            // TODO: Untested, and @Generated does compile for lambdas
            rollFromLines { lineReader.readLine(options.prompt) }
        } catch (e: BadHistoryException) {
            e.print()
        } catch (e: DiceException) {
            e.print()
        }
    }

    private fun Throwable.print() =
        options.commandLine.err.println(colorScheme.errorText(message))
}

val newRealRepl = NewRepl { options ->
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()

    val historyPath = Path(System.getProperty("user.home"), HISTORY_FILE_NAME)
    val lineReaderBuilder = lineReaderBuilder(terminal, options)
        .variable(HISTORY_FILE, historyPath)

    terminal to lineReaderBuilder
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
val newTestRepl = NewRepl { options ->
    val terminal = DumbTerminal(
        PROGRAM_NAME,
        if (inColor()) TYPE_DUMB_COLOR else TYPE_DUMB,
        System.`in`,
        System.out,
        UTF_8,
    )

    // Do not save test roll history to ~/.roll_history
    val lineReaderBuilder = lineReaderBuilder(terminal, options)
        .variable(HISTORY_FILE, createTempFile(PROGRAM_NAME))

    terminal to lineReaderBuilder
}

private fun lineReaderBuilder(
    terminal: Terminal,
    options: Options,
): LineReaderBuilder {
    val lineReaderBuilder = LineReaderBuilder.builder()
        .terminal(terminal)

    if (options.history) lineReaderBuilder
        .expander(RollingExpander())
    else lineReaderBuilder
        .variable(HISTORY_SIZE, 0)

    return lineReaderBuilder
}

private fun inColor() = AUTO.enabled()

/**
 * Only expand history when `!` is the first character in the line.
 * Dice expressions use `!` for explosion, not history expansion.
 * However, it is still handy to expand lines like `!!` or `!31`, etc.
 */
private class RollingExpander : DefaultExpander() {
    override fun expandHistory(history: History, line: String): String = try {
        if (!line.startsWith('!')) line
        else super.expandHistory(history, line)
    } catch (e: IllegalArgumentException) {
        throw BadHistoryException(e)
    }
}

// TODO: Horrible -- defeat JLine3 readLine()
private class BadHistoryException(cause: IllegalArgumentException) :
    Throwable(cause.message)
