package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

@Generated // Lie to JaCoCo
internal fun rollFromRepl(readerPrompt: String?): Int {
    val (terminal, replReader) = repl()
    terminal.use { // Terminals need closing to reset the external terminal
        try {
            while (true) rollFromLines {
                replReader.readLine(readerPrompt)
            }
        } catch (e: UserInterruptException) {
            return 130 // Shells return 130 on SIGINT
        } catch (e: EndOfFileException) {
            return 0
        }
    }
}

/**
 * @todo This may be better expressed a lazy property, however JaCoCo does not
 *       grok that, and `@Generated` doesn't work on properties
 */
@Generated // Lie to JaCoCo
private fun repl(): Pair<Terminal, LineReader> {
    val terminal = TerminalBuilder.builder()
        .name(PROGRAM_NAME)
        .build()
    val replReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .build()
    return terminal to replReader
}
