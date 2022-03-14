package hm.binkley.dice

import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import kotlin.random.Random

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    private val terminal: Terminal,
    private val options: Options,
) : MainRoller(random, reporter) {
    // TODO: Get rid of logic in favor of injection
    private val lineReader: LineReader =
        if (options.testRepl) options.testLineReader(terminal)
        else options.realLineReader(terminal)

    /** Note: closes (and resets) the terminal when done. */
    override fun rollAndReport() = terminal.use {
        while (true) try {
            // TODO: Untested, and @Generated does compile for lambdas
            rollFromLines { lineReader.readLine(options.prompt) }
        } catch (e: HistoryException) {
            e.printError()
        } catch (e: DiceException) {
            e.printError()
        }
    }

    private fun Throwable.printError() =
        options.commandLine.err.println(colorScheme.errorText(message))
}
