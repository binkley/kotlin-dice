package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme
import java.lang.System.err
import kotlin.random.Random

@Generated // Lie to JaCoCo
internal fun rollFromRepl(
    readerPrompt: String?,
    random: Random,
    reporter: MainReporter,
): Int {
    val (terminal, replReader) = repl()
    terminal.use { // Terminals need closing to reset the external terminal
        try {
            val colorScheme = defaultColorScheme(AUTO)
            while (true) try {
                rollFromLines(random, reporter) {
                    // TODO: JaCoCo says the lambda is untested, and does not
                    //       apply the @Generated from function scope
                    replReader.readLine(readerPrompt)
                }
            } catch (e: DiceException) {
                err.println(colorScheme.errorText(e.message))
            }
        } catch (e: UserInterruptException) {
            return 130 // Shells return 130 on SIGINT
        } catch (e: EndOfFileException) {
            return 0
        }
    }
}

/**
 * @todo This may be better expressed a lazy property, however JaCoCo does not
 *       grok that, and `@Generated` doesn't work on properties
 */
@Generated // Lie to JaCoCo
private fun repl(): Pair<Terminal, LineReader> {
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()
    val replReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    return terminal to replReader
}
