package hm.binkley.dice

import hm.binkley.dice.NeedsCommandLine.DoNeedsCommandLine
import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import hm.binkley.dice.NeedsSystemRegistry.DoNeedsSystemRegistry
import lombok.Generated
import org.jline.console.impl.SystemRegistryImpl.UnknownCommandException
import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import kotlin.random.Random

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    private val terminal: Terminal,
    private val options: Options,
) : MainRoller(random, reporter) {
    private val lineReader: LineReader = options.oldLineReader(terminal)

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

@Generated
class NewReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
) : MainRoller(random, reporter),
    NeedsCommandLine by DoNeedsCommandLine(),
    NeedsLineReader by DoNeedsLineReader(),
    NeedsSystemRegistry by DoNeedsSystemRegistry() {
    override fun rollAndReport() {
        while (true) try {
            val line = lineReader.readLine(options.prompt)

            if (line.maybeDiceExpression()) line.rollIt()
            else try {
                systemRegistry.execute(line)
            } finally {
                systemRegistry.cleanUp()
            }
        } catch (e: HistoryException) {
            e.printError()
        } catch (e: DiceException) {
            e.printError()
        } catch (e: UnknownCommandException) {
            e.printError()
        }
    }

    private fun Throwable.printError() =
        commandLine.err.println(colorScheme.errorText(message))
}
