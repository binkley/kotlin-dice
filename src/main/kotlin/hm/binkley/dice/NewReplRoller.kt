package hm.binkley.dice

import hm.binkley.dice.NeedsCommandLine.DoNeedsCommandLine
import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import hm.binkley.dice.NeedsSystemRegistry.DoNeedsSystemRegistry
import lombok.Generated
import org.jline.console.impl.SystemRegistryImpl.UnknownCommandException
import kotlin.random.Random

@Generated
class NewReplRoller(
    random: Random,
    reporter: MainReporter,
    private val options: Options,
) : MainRoller(random, reporter),
    NeedsCommandLine by DoNeedsCommandLine(),
    NeedsSystemRegistry by DoNeedsSystemRegistry(),
    NeedsLineReader by DoNeedsLineReader() {
    override fun rollAndReport() {
        while (true) try {
            val line = lineReader.readLine(options.prompt)

            if (line.maybeDiceExpression()) line.rollIt()
            else try {
                systemRegistry.execute(line)
            } finally {
                systemRegistry.cleanUp()
            }
        } catch (e: BadHistoryException) {
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
