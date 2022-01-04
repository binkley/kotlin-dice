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
import java.util.concurrent.Callable
import kotlin.random.Random
import kotlin.system.exitProcess

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

        return if (demo) {
            runDemo()
        } else if (arguments.isNotEmpty()) {
            rollFromArguments(arguments)
        } else if (null == System.console()) {
            rollFromStdin()
        } else {
            rollFromRepl(prompt)
        }
    }
}

private typealias ReadLine = () -> String?

private fun rollFromArguments(arguments: List<String>): Int {
    for (argument in arguments) {
        val result = rollIt(argument)
        if (0 != result) return result
    }
    return 0
}

private fun rollFromStdin() = rollFromLines { readLine() }

/**
 * @todo Don't rebuild the terminal & line reader each loop.  However, how to
 *       close the terminal when done (and reset the external command line)?
 */
@Generated // Lie to JaCoCo
private fun rollFromRepl(readerPrompt: String?): Int {
    TerminalBuilder.builder()
        .name("dice")
        .build().use { terminal ->
            try {
                val reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build()
                // Ignore failing exit code
                while (true) rollFromLines { reader.readLine(readerPrompt) }
            } catch (e: UserInterruptException) {
                return 130 // Shells return 130 on SIGINT
            } catch (e: EndOfFileException) {
                return 0
            }
        }
}

private fun rollFromLines(readLine: ReadLine): Int {
    do {
        val line = readLine()
        when {
            null == line -> return 0
            line.isEmpty() -> continue
            else -> {
                val result = rollIt(line)
                if (0 != result) return result
            }
        }
    } while (true)
}

private fun runDemo(): Int {
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

    return 0
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

fun rollIt(expression: String): Int {
    return if (noisy) rollNoisily(expression)
    else rollQuietly(expression)
}

private fun rollNoisily(expression: String): Int {
    println("---")
    println("Rolling $expression")
    val result = roll(expression, NoisyRolling, random)

    return if (!result.hasErrors()) {
        println("RESULT -> ${result.resultValue}")
        0
    } else {
        result.parseErrors.forEach {
            err.println(printParseError(it))
        }
        1
    }
}

private fun rollQuietly(expression: String): Int {
    val result = roll(expression, DoNothing, random)

    return if (!result.hasErrors()) {
        println("$expression ${result.resultValue}")
        0
    } else {
        result.parseErrors.forEach {
            err.println(printParseError(it))
        }
        1
    }
}
