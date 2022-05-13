package hm.binkley.dice.command

import hm.binkley.dice.NeedsCommandLine
import hm.binkley.dice.NeedsCommandLine.DoNeedsCommandLine
import hm.binkley.dice.NeedsLineReader
import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import lombok.Generated
import org.jline.reader.History
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(
    name = "history",
    description = ["list command history excluding this command"],
    mixinStandardHelpOptions = true,
)
@Generated
class HistoryCommand :
    Runnable,
    NeedsCommandLine by DoNeedsCommandLine(),
    NeedsLineReader by DoNeedsLineReader() {
    @Parameters(
        arity = "0..1",
        paramLabel = "N"
    )
    var take: Int = 20

    override fun run() = lineReader.history
        .reversed()
        .take(take + 1) // Do not count the history command itself
        .reversed()
        .forEach { commandLine.out.println(it.format()) }
}

/**
 * Formats history display the same as JLine3 does, but fixes the
 * off-by-one bug in JLine3 for displaying executed lines.
 *
 * **NB** &mdash; [History.Entry.index] is 0-based, and shell history
 * expansion is 1-based.
 */
@Generated
private fun History.Entry.format() = "%5d  %s".format(index() + 1, line())
