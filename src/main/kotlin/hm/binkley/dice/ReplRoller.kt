package hm.binkley.dice

import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import hm.binkley.dice.NeedsSystemRegistry.DoNeedsSystemRegistry
import lombok.Generated
import org.jline.console.impl.SystemRegistryImpl.UnknownCommandException
import kotlin.random.Random

sealed class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
) : MainRoller(random, reporter),
    NeedsLineReader by DoNeedsLineReader() {
    protected abstract fun String.maybeRoll()

    /**
     * Note: closes (and resets) the terminal when done.
     *
     * @todo Undo the god-object anti-pattern
     */
    final override fun rollAndReport() = options.terminal.use {
        while (true) {
            try {
                readLine().maybeRoll()
            } catch (e: HistoryException) {
                e.printError()
            } catch (e: DiceException) {
                e.printError()
            } catch (e: UnknownCommandException) {
                e.printError()
            }
        }
    }

    /** @todo Undo the god-object anti-pattern */
    private fun readLine() = lineReader.readLine(options.prompt)

    /** @todo Undo the god-object anti-pattern */
    private fun Throwable.printError() =
        options.commandLine.err.println(colorScheme.errorText(message))
}

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
) : ReplRoller(random, reporter, options) {
    override var lineReader = options.oldLineReader()

    override fun String.maybeRoll() {
        if (isNotEmpty()) roll()
    }
}

@Generated
class NewReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
) : ReplRoller(random, reporter, options),
    NeedsSystemRegistry by DoNeedsSystemRegistry() {
    override fun String.maybeRoll() {
        if (maybeDiceExpression()) roll()
        else try {
            systemRegistry.execute(this)
        } finally {
            systemRegistry.cleanUp()
        }
    }
}
