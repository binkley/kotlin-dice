package hm.binkley.dice

import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReader.HISTORY_FILE
import org.jline.reader.LineReader.HISTORY_SIZE
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultExpander
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import picocli.CommandLine
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.IExecutionStrategy
import picocli.CommandLine.RunLast
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.Path
import kotlin.io.path.createTempFile

interface ReplSupport : Runnable {
    val commandLine: CommandLine
    val color: ColorOption
    val debug: Boolean
    val history: Boolean
    val prompt: String
    val testRepl: Boolean
}

internal fun isInteractive() = null != System.console()

internal fun newRealTerminal() = TerminalBuilder.builder()
    .name(PROGRAM_NAME)
    .build()

internal fun newDumbTerminal() = DumbTerminal(
    PROGRAM_NAME,
    if (Ansi.AUTO.enabled()) TYPE_DUMB_COLOR else TYPE_DUMB,
    System.`in`,
    System.out,
    UTF_8,
)

internal fun ReplSupport.exceptionHandler() =
    IExecutionExceptionHandler { ex, commandLine, _ ->
        when (ex) {
            // User-friendly error message
            is DiceException -> {
                if (debug) commandLine.err.println(
                    colorScheme.richStackTraceString(ex)
                )
                else commandLine.err.println(
                    colorScheme.errorText(ex.message.maybeGnuPrefix())
                )
                commandLine.commandSpec.exitCodeOnExecutionException() // 1
            }
            // REPL closed with Ctrl-D/Ctrl-Z
            is EndOfFileException -> 0
            // Special case for the REPL - shells return 130 on SIGINT
            is UserInterruptException -> 130
            // Unknown exceptions fall back to Picolo default handling
            else -> throw ex
        }
    }

internal fun String?.maybeGnuPrefix(): String {
    // Be careful with null handling: an NPE will have no error message.
    // GNU standards prefix error messages with program name to aid in
    // debugging pipelines, etc.
    val message = this ?: ""
    return if (isInteractive()) message else "$PROGRAM_NAME: $message"
}

internal fun ReplSupport.executionStrategy() =
    IExecutionStrategy { parseResult ->
        // Run here rather than in Options so that --help respects the option
        color.install()

        RunLast().execute(parseResult)
    }

internal fun newRealRepl(options: ReplSupport): Pair<Terminal, LineReader> {
    val terminal = newRealTerminal()
    val lineReaderBuilder = lineReaderBuilder(terminal, options)

    // Do not save test rolls to ~/.roll_history; delete after finishing
    if (options.history) lineReaderBuilder
        .variable(HISTORY_FILE,
            Path(System.getProperty("user.home"), HISTORY_FILE_NAME))

    return terminal to lineReaderBuilder.build()
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
internal fun newTestRepl(options: ReplSupport): Pair<Terminal, LineReader> {
    val terminal = newDumbTerminal()
    val lineReaderBuilder = lineReaderBuilder(terminal, options)

    // Do not save test rolls to ~/.roll_history; delete after finishing
    if (options.history) lineReaderBuilder
        .variable(HISTORY_FILE, createTempFile(PROGRAM_NAME))

    return terminal to lineReaderBuilder.build()
}

private fun lineReaderBuilder(
    terminal: Terminal,
    options: ReplSupport,
): LineReaderBuilder {
    val lineReaderBuilder = LineReaderBuilder.builder()
        .terminal(terminal)

    if (options.history) lineReaderBuilder
        .expander(RollingExpander())
    else lineReaderBuilder
        .variable(HISTORY_SIZE, 0)

    return lineReaderBuilder
}

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
internal class BadHistoryException(cause: IllegalArgumentException) :
    Throwable(cause.message)
