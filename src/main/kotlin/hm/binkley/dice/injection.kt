package hm.binkley.dice

import lombok.Generated
import org.jline.console.SystemRegistry
import org.jline.reader.LineReader
import org.jline.terminal.Terminal
import picocli.CommandLine

interface NeedsTerminal {
    var terminal: Terminal

    @Generated
    class DoNeedsTerminal : NeedsTerminal {
        override lateinit var terminal: Terminal
    }
}

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

fun <T> T.inject(
    commandLine: CommandLine,
    terminal: Terminal,
    systemRegistry: SystemRegistry?,
    lineReader: LineReader,
): T = apply {
    if (this is NeedsTerminal) this.terminal = terminal
    if (this is NeedsCommandLine) this.commandLine = commandLine
    // TODO: Remove the null check when old REPL goes away (needed by Options)
    // TODO: If options is manually set up ... why does injection happen?
    if (null != systemRegistry && this is NeedsSystemRegistry)
        this.systemRegistry = systemRegistry
    if (this is NeedsLineReader) this.lineReader = lineReader
}

@Generated
fun CommandLine.inject(
    commandLine: CommandLine,
    terminal: Terminal,
    systemRegistry: SystemRegistry?,
    lineReader: LineReader,
): CommandLine = apply {
    val command = getCommand<Any>()
    command.inject(commandLine, terminal, systemRegistry, lineReader)

    // Recur through subcommands
    subcommands.map { it.value }.forEach {
        it.inject(commandLine, terminal, systemRegistry, lineReader)
    }
}
