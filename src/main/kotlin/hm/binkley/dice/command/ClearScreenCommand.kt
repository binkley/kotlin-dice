package hm.binkley.dice.command

import hm.binkley.dice.NeedsLineReader
import hm.binkley.dice.NeedsLineReader.DoNeedsLineReader
import lombok.Generated
import org.jline.utils.InfoCmp.Capability.clear_screen
import picocli.CommandLine.Command

@Command(
    name = "clear",
    description = ["clear the screen"],
    mixinStandardHelpOptions = true,
)
@Generated
class ClearScreenCommand :
    Runnable,
    NeedsLineReader by DoNeedsLineReader() {
    override fun run() {
        lineReader.terminal.puts(clear_screen)
    }
}
