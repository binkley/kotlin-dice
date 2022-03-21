package hm.binkley.dice

import lombok.Generated
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl.UnknownCommandException
import kotlin.random.Random

fun Options.pickReplRoller(random: Random, reporter: MainReporter) =
    if (newRepl) NewReplRoller(random, reporter, this, systemRegistry)
    else OldReplRoller(random, reporter, this)

sealed class ReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
) : MainRoller(random, reporter) {
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
    private fun readLine() = options.lineReader.readLine(options.prompt)

    /** @todo Undo the god-object anti-pattern */
    private fun Throwable.printError() =
        options.commandLine.err.println(colorScheme.errorText(message))
}

class OldReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
) : ReplRoller(random, reporter, options) {
    override fun String.maybeRoll() {
        if (isNotEmpty()) roll()
    }
}

@Generated
class NewReplRoller(
    random: Random,
    reporter: MainReporter,
    options: Options,
    private val systemRegistry: SystemRegistry,
) : ReplRoller(random, reporter, options) {
    override fun String.maybeRoll() {
        if (maybeDiceExpression()) roll()
        else try {
            systemRegistry.execute(this)
        } finally {
            systemRegistry.cleanUp()
        }
    }
}
