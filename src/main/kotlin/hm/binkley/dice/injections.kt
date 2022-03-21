package hm.binkley.dice

import lombok.Generated
import org.jline.reader.LineReader
import picocli.CommandLine

interface NeedsCommandLine {
    var commandLine: CommandLine

    @Generated
    class DoNeedsCommandLine : NeedsCommandLine {
        override lateinit var commandLine: CommandLine
    }
}

interface NeedsLineReader {
    var lineReader: LineReader

    @Generated
    class DoNeedsLineReader : NeedsLineReader {
        override lateinit var lineReader: LineReader
    }
}

fun CommandLine.inject(
    commandLine: CommandLine,
    lineReader: LineReader,
): CommandLine = apply {
    // TODO: Why does these need injecting here?  Else it blows with an
    //       exception when running the new REPL live.  And what does it mean
    //       to call this on subcommands?
    getCommand<Options>().inject(commandLine, lineReader)

    // Recur through subcommands
    subcommands.map { it.value }.forEach {
        it.inject(commandLine, lineReader)
    }
}

private fun <T> T.inject(
    commandLine: CommandLine,
    lineReader: LineReader,
): T = apply {
    if (this is NeedsCommandLine) this.commandLine = commandLine
    if (this is NeedsLineReader) this.lineReader = lineReader
}
