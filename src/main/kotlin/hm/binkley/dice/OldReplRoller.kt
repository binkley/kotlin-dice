package hm.binkley.dice

import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import kotlin.random.Random

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
) : MainRoller(random, reporter) {
    private val terminal: Terminal
    private val lineReader: LineReader

    init {
        // TODO: Get rid init block in favor of injection
        val (terminal, lineReader) =
            if (options.testRepl) newTestRepl(options)
            else newRealRepl(options)
        this.terminal = terminal
        this.lineReader = lineReader
    }

    /** Note: closes (and resets) the terminal when done. */
    override fun rollAndReport() = terminal.use {
        while (true) try {
            // TODO: Untested, and @Generated does compile for lambdas
            rollFromLines { lineReader.readLine(options.prompt) }
        } catch (e: BadHistoryException) {
            e.printError()
        } catch (e: DiceException) {
            e.printError()
        }
    }

    private fun Throwable.printError() =
        options.commandLine.err.println(colorScheme.errorText(message))
}
