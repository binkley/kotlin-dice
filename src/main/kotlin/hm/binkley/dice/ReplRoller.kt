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
    /**
     * Note: closes (and resets) the terminal when done.
     *
     * @todo Undo the god-object anti-pattern
     */
    final override fun rollAndReport() = options.terminal.use {
        while (true) {
            try {
                rollSome()
            } catch (e: HistoryException) {
                e.printError()
            } catch (e: DiceException) {
                e.printError()
            } catch (e: UnknownCommandException) {
                e.printError()
            }
        }
    }

    protected fun readLine(): String = lineReader.readLine(options.prompt)

    /** @todo Undo the god-object anti-pattern */
    private fun Throwable.printError() =
        options.commandLine.err.println(colorScheme.errorText(message))

    protected abstract fun rollSome()
}

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
) : ReplRoller(random, reporter, options) {
    override var lineReader = options.oldLineReader(options.terminal)

    /** @todo `rollFromLines` is a pull-loop: why loop twice (superclass)? */
    override fun rollSome() = rollFromLines { readLine() }
}

@Generated
class NewReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
) : ReplRoller(random, reporter, options),
    NeedsSystemRegistry by DoNeedsSystemRegistry() {
    override fun rollSome() {
        val line = readLine()
        if (line.maybeDiceExpression()) line.rollIt()
        else try {
            systemRegistry.execute(line)
        } finally {
            systemRegistry.cleanUp()
        }
    }
}
