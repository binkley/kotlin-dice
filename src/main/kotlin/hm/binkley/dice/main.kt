package hm.binkley.dice

import lombok.Generated
import org.fusesource.jansi.AnsiConsole
import org.jline.console.CmdLine
import org.jline.console.SystemRegistry
import org.jline.console.impl.Builtins
import org.jline.console.impl.Builtins.Command.TTOP
import org.jline.console.impl.SystemRegistryImpl
import org.jline.keymap.KeyMap
import org.jline.reader.Binding
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReader.LIST_MAX
import org.jline.reader.LineReaderBuilder
import org.jline.reader.MaskingCallback
import org.jline.reader.Parser
import org.jline.reader.Reference
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.jline.widget.TailTipWidgets
import org.jline.widget.TailTipWidgets.TipType.COMPLETER
import org.parboiled.errors.ErrorUtils.printParseError
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.HelpCommand
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.shell.jline3.PicocliCommands
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory
import java.io.PrintWriter
import java.lang.System.err
import java.lang.System.out
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.function.Supplier
import kotlin.system.exitProcess

@Generated // Lie to JaCoCo
fun main(args: Array<String>) {
    AnsiConsole.systemInstall()
    val exitCode = try {
        CommandLine(Options()).execute(*args)
    } finally {
        AnsiConsole.systemUninstall()
    }
    // Funny construction so that JAnsi can clean up the terminal before we
    // call exit().  Normally this would be in the "try" block
    exitProcess(exitCode)
}

private fun readShell() {
    // TODO: Not used by dice roller -- needed by JLine
    val workDir = Supplier {
        Paths.get(System.getProperty("user.dir"))
    }

    // set up JLine built-in commands
    val builtins = Builtins(workDir, null, null)
    // TODO: "top" should not be a cmd, but execs /usr/bin/top (!!)
    builtins.rename(TTOP, "top")
    builtins.alias("bindkey", "keymap")

    // set up picocli commands
    val commands = CliCommands()
    val factory = PicocliCommandsFactory()
    // Or, if you have your own factory, you can chain them like this:
    // MyCustomFactory customFactory = createCustomFactory(); // your application custom factory
    // PicocliCommandsFactory factory = new PicocliCommandsFactory(customFactory); // chain the factories
    val cmd = CommandLine(commands, factory)
    val picocliCommands = PicocliCommands(cmd)

    val parser: Parser = DefaultParser()
    TerminalBuilder.builder().build().use { terminal ->
        val systemRegistry: SystemRegistry = SystemRegistryImpl(
            parser, terminal, workDir, null
        )
        systemRegistry.setCommandRegistries(builtins, picocliCommands)
        systemRegistry.register("help", picocliCommands)

        val reader: LineReader =
            LineReaderBuilder.builder().terminal(terminal)
                .completer(systemRegistry.completer()).parser(parser)
                .variable(LIST_MAX, 50) // max tab completion candidates
                .build()
        builtins.setLineReader(reader)
        commands.setReader(reader)
        factory.setTerminal(terminal)

        // TODO: Do widgets make sense in context of dice rolling?
        val widgets = TailTipWidgets(
            reader, { line: CmdLine? ->
                systemRegistry.commandDescription(line)
            }, 5, COMPLETER
        )
        widgets.enable()

        val keyMap: KeyMap<Binding> = reader.keyMaps["main"]!!
        keyMap.bind(Reference("tailtip-toggle"), KeyMap.alt("s"))

        // TODO: Decide on a good prompt for rolling dice
        val prompt = "& "
        val rightPrompt: String? = null

        // start the shell and process input until the user quits with Ctrl-D
        var line: String?
        while (true) {
            try {
                systemRegistry.cleanUp()
                line = reader.readLine(
                    prompt, rightPrompt, null as MaskingCallback?, null
                )
                systemRegistry.execute(line)
            } catch (e: UserInterruptException) {
                // TODO: Exit -- status 0 or an error status?
            } catch (e: EndOfFileException) {
                return // TODO: To exit with error status, should be return 0
            } catch (e: Exception) {
                systemRegistry.trace(e)
            }
        }
    }
}

@Command(
    name = "",
    description = [
        "Role dice. Use @|magenta <TAB>|@ to see available commands.",
        "Enter a dice expression to see a roll outcome."
    ],
    footer = ["Press Ctrl-D to exit."],
    subcommands = [HelpCommand::class]
)
private class CliCommands : Runnable {
    var out: PrintWriter? = null

    fun setReader(reader: LineReader) {
        out = reader.terminal.writer()
    }

    override fun run() {
        out!!.println(CommandLine(this).usageMessage)
    }
}

private fun runDemo() {
    roll("D6")
    roll("z6")
    roll("3d6")
    roll("3z6")
    roll("3d6+1")
    roll("3d6-1")
    roll("10d3!")
    roll("10d3!2")
    roll("4d6h3")
    roll("4d6l3")
    roll("3d6+2d4")
    roll("d%")
    roll("6d4l5!")
    roll("3d12r1h2!11")
    roll("blah")
    println("DONE") // Show that bad expression did not throw
}

private var noisy = false

@Generated // Lie to JaCoCo
private val NoisyRolling = OnRoll {
    println(
        when (it) {
            is PlainRoll -> "roll(d${it.d}) -> ${it.roll}"
            is PlainReroll -> "reroll(d${it.d}) -> ${it.roll}"
            is ExplodedRoll -> "!roll(d${it.d}) -> ${it.roll}"
            is ExplodedReroll -> "!reroll(d${it.d}) -> ${it.roll}"
            is DroppedRoll -> "drop -> ${it.roll}"
        }
    )
}

@Generated
fun roll(expression: String) {
    if (noisy) rollNoisily(expression)
    else rollQuietly(expression)
}

@Generated // Lie to JaCoCo
private fun rollNoisily(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = roll(expression, NoisyRolling)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors()) println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}

@Generated // Lie to JaCoCo
private fun rollQuietly(expression: String) {
    val result = roll(expression, DoNothing)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors()) println("$expression ${result.resultValue}")
    err.flush()
    out.flush()
}

@Command(
    name = "dice",
    mixinStandardHelpOptions = true,
    version = ["dice 0-SNAPSHOT"]
)
@Generated // Lie to JaCoCo -- TODO: tests for CLI
private class Options : Callable<Int> {
    @Option(names = ["--demo"])
    var demo = false

    @Option(names = ["-v", "--verbose"])
    var verbose = false

    @Parameters
    var parameters: List<String> = emptyList()

    override fun call(): Int {
        noisy = verbose

        if (demo) {
            runDemo()
        } else if (parameters.isEmpty()) {
            readShell()
        } else {
            for (expression in parameters) roll(expression)
        }

        return 0
    }
}
