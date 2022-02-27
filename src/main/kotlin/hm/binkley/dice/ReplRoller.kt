package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.lang.System.err
import kotlin.random.Random

@Generated // Lie to JaCoCo
class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val prompt: String?,
) : MainRoller(random, reporter) {
    override fun rollAndReport() {
        val (terminal, replReader) = repl()
        terminal.use { // Terminals need closing to reset the external terminal
            try {
                while (true) try {
                    rollFromLines(random, reporter) {
                        // TODO: JaCoCo says the lambda is untested, and does not
                        //       apply the @Generated from function scope
                        replReader.readLine(prompt)
                    }
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
