package hm.binkley.dice

import lombok.Generated
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import org.parboiled.errors.ErrorUtils.printParseError
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.lang.System.err
import java.lang.System.out
import java.util.concurrent.Callable
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * @todo Exit process with non-0 if using pipe or cmd line, and parsing fails
 */
@Generated // Lie to JaCoCo -- use of exit confuses it
fun main(args: Array<String>): Unit =
    exitProcess(CommandLine(Options()).execute(*args))

@Command(
    name = "dice",
    mixinStandardHelpOptions = true,
    version = ["dice 0-SNAPSHOT"]
)
@Generated // Lie to JaCoCo
private class Options : Callable<Int> {
    @Option(
        names = ["--demo"],
        description = ["Run the demo; ignore arguments."],
    )
    var demo = false

    @Option(
        names = ["-p", "--prompt"],
        description = ["Change the interactive prompt from '\uD83C\uDFB2 '."],
    )
    var prompt = "\uD83C\uDFB2 " // Colorful die

    @Option(
        names = ["-s", "--seed"],
        description = ["Provide a random seed for repeatable results."],
    )
    var seed: Int? = null

    @Option(
        names = ["-v", "--verbose"],
        description = ["Explain each die roll as it happens."],
    )
    var verbose = false

    @Parameters(
        description = ["Dice expressions to roll",
            "If none provided, prompt user interactively"],
    )
    var arguments: List<String> = emptyList()

    override fun call(): Int {
        noisy = verbose
        // TODO: Why does Kotlin require non-null assertion?
        if (null != seed) random = Random(seed!!)

        if (demo) {
            runDemo()
        } else if (arguments.isNotEmpty()) {
            rollFromArguments(arguments)
        } else if (null == System.console()) {
            rollFromStdin()
        } else {
            rollFromRepl(prompt)
        }

        return 0
    }
}

private typealias ReadLine = () -> String?

private fun rollFromArguments(arguments: List<String>) =
    arguments.forEach { rollIt(it) }

private fun rollFromStdin() = rollFromLines { readLine() }

/**
 * @todo Don't rebuild the terminal & line reader each loop.  However, how to
 *       close the terminal when done (and reset the external command line)?
 */
@Generated // Lie to JaCoCo
private fun rollFromRepl(readerPrompt: String?) = TerminalBuilder.builder()
    .name("dice")
    .build().use { terminal ->
        try {
            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build()
            rollFromLines { reader.readLine(readerPrompt) }
        } catch (e: UserInterruptException) {
            return // TODO: Should we complain on ^C?
        } catch (e: EndOfFileException) {
            return
        }
    }

private fun rollFromLines(readLine: ReadLine) {
    do {
        val line = readLine()
        when {
            null == line -> return
            line.isEmpty() -> continue
            else -> rollIt(line)
        }
    } while (true)
}

private fun runDemo() {
    for (expression in arrayOf(
        "D6",
        "z6",
        "3d6",
        "3z6",
        "3d6+1",
        "3d6-1",
        "10d3!",
        "10d3!2",
        "4d6h3",
        "4d6l3",
        "3d6+2d4",
        "d%",
        "6d4l5!",
        "3d12r1h2!11",
        "blah",
    ))
        rollIt(expression)

    println("DONE") // Show that bad expression did not throw
}

private var noisy = false
private var random: Random = Random.Default

private val NoisyRolling = OnRoll {
    // TODO: Colorize output when using a prompt
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

fun rollIt(expression: String) {
    if (noisy) rollNoisily(expression)
    else rollQuietly(expression)
}

private fun rollNoisily(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = roll(expression, NoisyRolling, random)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors()) println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}

private fun rollQuietly(expression: String) {
    val result = roll(expression, DoNothing, random)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors()) println("$expression ${result.resultValue}")
    err.flush()
    out.flush()
}
