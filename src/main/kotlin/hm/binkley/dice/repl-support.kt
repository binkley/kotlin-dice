package hm.binkley.dice

import lombok.Generated
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReader.HISTORY_FILE
import org.jline.reader.LineReader.HISTORY_SIZE
import org.jline.reader.LineReader.SuggestionType.COMPLETER
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultExpander
import org.jline.reader.impl.DefaultParser
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
import picocli.shell.jline3.PicocliCommands
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.text.RegexOption.IGNORE_CASE

fun isInteractive() = null != System.console()

/**
 * All dice expressions start either with a non-zero digit, or the letters
 * 'd' or 'z'.  No commands start with 'd' or 'z'.
 */
private val diceLike = Regex("^[1-9dz]", IGNORE_CASE)

fun String.maybeDiceExpression() =
    diceLike.containsMatchIn(trimStart())

fun Options.commandLineAndTerminal(
    args: Array<String>,
): Pair<CommandLine, Terminal> {
    val commandLine = commandLine.apply { parseArgs(*args) }
    // Parse command line first to respect --test-repl option
    val terminal =
        if (testRepl) dumbTerminal()
        else realTerminal()

    return commandLine to terminal
}

fun realTerminal(): Terminal = TerminalBuilder.builder()
    .name(PROGRAM_NAME)
    .build()

private fun dumbTerminal() = DumbTerminal(
    PROGRAM_NAME,
    if (Ansi.AUTO.enabled()) TYPE_DUMB_COLOR else TYPE_DUMB,
    System.`in`,
    System.out,
    UTF_8,
)

fun Options.exceptionHandler() =
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

fun String?.maybeGnuPrefix(): String {
    // Be careful with null handling: an NPE will have no error message.
    // GNU standards prefix error messages with program name to aid in
    // debugging pipelines, etc.
    val message = this ?: ""
    return if (isInteractive()) message else "$PROGRAM_NAME: $message"
}

fun Options.executionStrategy() =
    IExecutionStrategy { parseResult ->
        // Run here rather than in Options so that --help respects the option
        color.install()

        RunLast().execute(parseResult)
    }

@Generated
fun CommandLine.parserAndSystemRegistry(
    terminal: Terminal,
): Pair<Parser, SystemRegistryImpl> {
    val parser: Parser = DefaultParser()
    val systemRegistry = SystemRegistryImpl(parser, terminal, null, null)
        .groupCommandsInHelp(false)
    systemRegistry.setCommandRegistries(PicocliCommands(this))

    return parser to systemRegistry
}

fun Options.realLineReader(terminal: Terminal): LineReader {
    val builder = lineReaderBuilder(terminal, this)

    // Save REPL rolls to ~/.roll_history
    if (history) builder.variable(
        HISTORY_FILE,
        Path(System.getProperty("user.home"), HISTORY_FILE_NAME)
    )

    return builder.build()
}

/**
 * The terminal builder hands file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, and provides no means for changing them.
 */
fun Options.testLineReader(terminal: Terminal): LineReader {
    val builder = lineReaderBuilder(terminal, this)

    // Do not save test rolls to ~/.roll_history; delete after finishing
    if (history) builder.variable(HISTORY_FILE, createTempFile(PROGRAM_NAME))

    return builder.build()
}

/**
 * @todo No pipelines or am/or shell operators
 * @todo Alias "quit" to "exit"
 */
@Generated
fun lineReader(
    terminal: Terminal,
    systemRegistry: SystemRegistry,
    parser: Parser,
): LineReader = LineReaderBuilder.builder()
    .completer(systemRegistry.completer())
    .expander(RollingExpander)
    .parser(parser)
    .terminal(terminal)
    .build().apply {
        autosuggestion = COMPLETER
    }

private fun lineReaderBuilder(
    terminal: Terminal,
    options: Options,
) = LineReaderBuilder.builder().apply {
    terminal(terminal)
    if (options.history) expander(RollingExpander)
    else variable(HISTORY_SIZE, 0)
}

/**
 * Only expand history when `!` is the first character in the line.
 * Dice expressions use `!` for explosion, not history expansion.
 * However, it is still handy to expand lines like `!!` or `!31`, etc.
 *
 * @todo Some better way than catching IAE
 */
private object RollingExpander : DefaultExpander() {
    override fun expandHistory(history: History, line: String): String = try {
        if (line.maybeDiceExpression()) line
        else super.expandHistory(history, line)
    } catch (e: IllegalArgumentException) {
        throw BadHistoryException(e)
    }
}

/**
 * Extends `Throwable` so that it is neither an `Exception` nor an `Error`.
 * This workaround defeats JLine3 from force-dumping a stack trace.
 */
class BadHistoryException(cause: IllegalArgumentException) :
    Throwable(cause.message)
