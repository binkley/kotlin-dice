package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.TYPE_DUMB
import org.jline.terminal.Terminal.TYPE_DUMB_COLOR
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import picocli.CommandLine.Help.Ansi.AUTO
import java.lang.System.err
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.random.Random

class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val prompt: String,
    newTerminal: () -> Terminal,
) : MainRoller(random, reporter) {
    private val terminal = newTerminal()
    private val lineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()

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
fun terminal(): Terminal = TerminalBuilder.builder()
    .name(PROGRAM_NAME)
    .build()

/**
 * The terminal build hands the file descriptors for STDIN and STDOUT to the
 * constructor of dumb terminals, so cannot change them out for testing.
 */
fun testTerminal() = DumbTerminal(
    PROGRAM_NAME,
    if (AUTO.enabled()) TYPE_DUMB_COLOR else TYPE_DUMB,
    System.`in`,
    System.out,
    UTF_8,
)
