package hm.binkley.dice

import lombok.Generated
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable
import kotlin.random.Random
import kotlin.system.exitProcess

internal const val PROGRAM_NAME = "dice"

@Generated // Lie to JaCoCo -- use of exit confuses it
fun main(args: Array<String>): Unit =
    exitProcess(CommandLine(Options()).execute(*args))

@Command(
    name = PROGRAM_NAME,
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
        fun Boolean.reporter(colored: Boolean) =
            selectMainReporter(this, colored)

        // TODO: Why does Kotlin require non-null assertion?
        if (null != seed) random = Random(seed!!)

        // TODO: Pass reporters to "roll" methods
        return if (demo) {
            rollForDemo(verbose.reporter(false))
        } else if (arguments.isNotEmpty()) {
            rollFromArguments(arguments, verbose.reporter(false))
        } else if (null == System.console()) {
            rollFromStdin(verbose.reporter(false))
        } else {
            rollFromRepl(prompt, verbose.reporter(true))
        }
    }
}

private fun rollFromArguments(
    arguments: List<String>, reporter: MainReporter
): Int {
    for (argument in arguments) {
        val result = rollIt(argument, reporter)
        if (0 != result) return result
    }
    return 0
}

private fun rollFromStdin(reporter: MainReporter) =
    rollFromLines({ readLine() }, reporter)

private typealias ReadLine = () -> String?

internal fun rollFromLines(readLine: ReadLine, reporter: MainReporter): Int {
    do {
        val line = readLine()
        when {
            null == line -> return 0
            line.isEmpty() -> continue
            else -> {
                val result = rollIt(line, reporter)
                if (0 != result) return result
            }
        }
    } while (true)
}

private fun rollForDemo(reporter: MainReporter): Int {
    for (expression in demoExpressions) {
        // TODO: Teach [MainReporter] about verbosity, else a method there
        if (reporter is UncoloredVerboseReporter
            || reporter is ColoredVerboseReporter
        )
            println("---")
        rollIt(expression.first, reporter)
    }

    println("DONE") // Show that bad expression did not throw

    return 0
}

private var random: Random = Random.Default

private fun rollIt(expression: String, reporter: MainReporter): Int {
    val result = roll(expression, random, reporter)

    reporter.display(result)

    return if (!result.hasErrors()) 0 else 1
}

/**
 * Used by both demo and testing.
 * The second value is the expectation given a seed of 1 (used by testing).
 */
internal val demoExpressions = arrayOf(
    "D6" to 4,
    "z6" to 3,
    "Z6" to 3,
    "3d6" to 10,
    "3D6" to 10,
    "3d6+1" to 11, // whitespace example
    "3d6+ 1" to 11, // whitespace example
    "3d6 +1" to 11, // whitespace example
    "3d6 + 1" to 11, // whitespace example
    "3d6-1" to 9,
    "10d3!" to 20,
    "10d3!2" to 49,
    "4d6h3" to 10,
    "4d6H3" to 10,
    "4d6l3" to 6,
    "4d6L3" to 6,
    "3d6+2d4" to 17,
    "d%" to 66,
    // Constant seed keeps the role constant, so z% is one less than d%
    "z%" to 65,
    "6d4l5!" to 20,
    "3d3r1h2!" to 10,
    "3d12!10" to 23,
    "100d3r1h99!+100d3r1l99!3-17" to 919,
    "blah" to null,
)
