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
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory
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

fun Options.parseOptions(vararg args: String): CommandLine {
    // As Options bootstraps everything else, use setter rather than injection
    commandLine = CommandLine(this, PicocliCommandsFactory())
        // Use the last setting for an option if it is repeated; needed
        // testing which defaults to no color, but some tests will
        // override the option to test color output
        .setOverwrittenOptionsAllowed(true)
        .setExecutionStrategy(executionStrategy())
        .setExecutionExceptionHandler(exceptionHandler())
        .apply { parseArgs(*args) }
    terminal =
        if (testRepl) dumbTerminal()
        else realTerminal()

    // TODO: Restructure code ordering
    prepareRepl(commandLine)

    return commandLine
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

private fun Options.prepareRepl(commandLine: CommandLine) {
    if (newRepl) prepareNewRepl(commandLine)
    else prepareOldRepl(commandLine)
}

private fun Options.prepareOldRepl(commandLine: CommandLine) {
    val lineReader = oldLineReader(terminal)
    commandLine.inject(commandLine, terminal, null, lineReader)
}

private fun Options.prepareNewRepl(commandLine: CommandLine) {
    val (systemRegistry, parser) = commandLine
        .systemRegistryAndParser(terminal)
    val lineReader = newLineReader(terminal, systemRegistry, parser)
    commandLine.inject(commandLine, terminal, systemRegistry, lineReader)
}

private fun CommandLine.systemRegistryAndParser(
    terminal: Terminal,
): Pair<SystemRegistry, Parser> {
    val parser: Parser = DefaultParser()
    val systemRegistry = SystemRegistryImpl(parser, terminal, null, null)
        .groupCommandsInHelp(false)
    systemRegistry.setCommandRegistries(PicocliCommands(this))

    return systemRegistry to parser
}

fun Options.oldLineReader(terminal: Terminal): LineReader {
    val builder = lineReaderBuilder(terminal)

    if (history)
        if (testRepl) {
            // Do not save test rolls to ~/.roll_history; delete after finishing
            builder.variable(HISTORY_FILE, createTempFile(PROGRAM_NAME))
        } else
        // Save REPL rolls to ~/.roll_history
            builder.variable(
                HISTORY_FILE,
                Path(System.getProperty("user.home"), HISTORY_FILE_NAME)
            )

    return builder.build()
}

/**
 * @todo No pipelines or am/or shell operators
 * @todo Alias "quit" to "exit"
 * @todo Reconcile the *three* factory functions for two purposes
 */
@Generated
fun Options.newLineReader(
    terminal: Terminal,
    systemRegistry: SystemRegistry,
    parser: Parser,
): LineReader = lineReaderBuilder(terminal)
    .completer(systemRegistry.completer())
    .parser(parser)
    .terminal(terminal)
    .build().apply {
        autosuggestion = COMPLETER
    }

private fun Options.lineReaderBuilder(
    terminal: Terminal,
) = LineReaderBuilder.builder().apply {
    terminal(terminal)
    expander(RollingExpander(this@lineReaderBuilder))
    if (!history) variable(HISTORY_SIZE, 0)
}

/**
 * Only expand history when `!` is the first character in the line.
 * Dice expressions use `!` for explosion, not history expansion.
 * However, it is still handy to expand lines like `!!` or `!31`, etc.
 *
 * @todo Some better way than catching IAE
 */
private class RollingExpander(
    private val options: Options,
) : DefaultExpander() {
    override fun expandHistory(history: History, line: String): String = try {
        if (line.maybeDiceExpression()) line
        else if (!options.history) throw HistoryDisabledException
        else super.expandHistory(history, line)
    } catch (e: IllegalArgumentException) {
        throw BadHistoryException(e)
    }
}

abstract class HistoryException(message: String) : Throwable(message)

/**
 * Extends `Throwable` so that it is neither an `Exception` nor an `Error`.
 * This workaround defeats JLine3 from force-dumping a stack trace.
 */
object HistoryDisabledException :
    HistoryException("History disabled because of the --no-history option")

/**
 * Extends `Throwable` so that it is neither an `Exception` nor an `Error`.
 * This workaround defeats JLine3 from force-dumping a stack trace.
 */
class BadHistoryException(cause: IllegalArgumentException) :
    HistoryException(cause.message!!)
