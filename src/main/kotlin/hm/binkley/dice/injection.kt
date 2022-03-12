package hm.binkley.dice

import lombok.Generated
import org.jline.console.SystemRegistry
import org.jline.reader.LineReader
import picocli.CommandLine

interface NeedsCommandLine {
    var commandLine: CommandLine

    @Generated
    class DoNeedsCommandLine : NeedsCommandLine {
        override lateinit var commandLine: CommandLine
    }
}

interface NeedsSystemRegistry {
    var systemRegistry: SystemRegistry

    @Generated
    class DoNeedsSystemRegistry : NeedsSystemRegistry {
        override lateinit var systemRegistry: SystemRegistry
    }
}

interface NeedsLineReader {
    var lineReader: LineReader

    @Generated
    class DoNeedsLineReader : NeedsLineReader {
        override lateinit var lineReader: LineReader
    }
}

@Generated
fun <T> T.inject(
    commandLine: CommandLine,
    lineReader: LineReader,
    systemRegistry: SystemRegistry,
): T = apply {
    if (this is NeedsCommandLine) this.commandLine = commandLine
    if (this is NeedsLineReader) this.lineReader = lineReader
    if (this is NeedsSystemRegistry)
        this.systemRegistry = systemRegistry
}

@Generated
fun CommandLine.inject(
    commandLine: CommandLine,
    lineReader: LineReader,
    systemRegistry: SystemRegistry,
): CommandLine = apply {
    val command = getCommand<Any>()
    command.inject(commandLine, lineReader, systemRegistry)

    // Recur through subcommands
    subcommands.map { it.value }.forEach {
        it.inject(commandLine, lineReader, systemRegistry)
    }
}
