package hm.binkley.dice

import lombok.Generated
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.IExecutionStrategy
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.CommandLine.RunLast
import java.lang.System.err
import kotlin.random.Random
import kotlin.system.exitProcess

internal const val PROGRAM_NAME = "roll"

@Generated // Lie to JaCoCo -- use of exit confuses it
fun main(args: Array<String>) {
    val simpleExceptionReporting =
        IExecutionExceptionHandler { ex, commandLine, _ ->
            with(commandLine) {
                err.println(colorScheme.errorText(ex.message))
                commandSpec.exitCodeOnExecutionException()// 1
            }
        }
    val options = Options()
    val forceColorIfRequested =
        IExecutionStrategy { parseResult ->
            if (options.color) System.setProperty("picocli.ansi", "true")
            RunLast().execute(parseResult)
        }

    exitProcess(
        CommandLine(options)
            .setExecutionExceptionHandler(simpleExceptionReporting)
            .setExecutionStrategy(forceColorIfRequested)
            .execute(*args)
    )
}

@Command(
    name = PROGRAM_NAME,
    mixinStandardHelpOptions = true,
    version = ["dice 0-SNAPSHOT"],
    footer = [
        """
Examples:
  @|bold roll|@
     Start the interactive dice rolling prompt.
  @|bold roll|@ <@|italic expression|@>
     Print result of dice expression, and exit.
  echo @|italic <expression>|@ | @|bold roll|@
     Print result of STDIN as a dice expression, and exit.

Exit codes:
  @|bold 0|@ - Successful completion
  @|bold 1|@ - Bad dice expression
  @|bold 2|@ - Bad program usage
        """
    ],
)
@Generated // Lie to JaCoCo
private class Options : Runnable {
    /** @todo Support GNU `--color[=WHEN]` */
    @Option(
        description = ["Force color output",
            "The demo, command-line arguments, and piped input default to no color",
            "The REPL defaults to color based on terminal support"],
        names = ["-C", "--color"],
    )
    var color = false

    @Option(
        description = ["Run the demo; ignore arguments."],
        names = ["--demo"],
    )
    var demo = false

    @Option(
        description = ["Fail results below a minimum."],
        names = ["-m", "--minimum"],
        paramLabel = "MINIMUM",
    )
    var minimum = Int.MIN_VALUE

    @Option(
        description = ["Change the interactive prompt from '\uD83C\uDFB2 '."],
        names = ["-p", "--prompt"],
        paramLabel = "PROMPT",
    )
    var prompt = "\uD83C\uDFB2 " // Colorful die

    @Option(
        description = ["Provide a random seed for repeatable results."],
        names = ["-s", "--seed"],
        paramLabel = "SEED",
    )
    var seed: Int? = null

    @Option(
        description = ["Explain each die roll as it happens."],
        names = ["-v", "--verbose"],
    )
    var verbose = false

    @Parameters(
        description = ["Dice expressions to roll",
            "If none provided, prompt user interactively"],
        paramLabel = "EXPRESSION",
    )
    var arguments: List<String> = emptyList()

    override fun run() {
        // TODO: Why does Kotlin require non-null assertion?
        val random = if (null == seed) Random.Default else Random(seed!!)
        val reporter = selectMainReporter(minimum, verbose)

        when {
            demo -> rollForDemo(random, reporter)
            arguments.isNotEmpty() ->
                rollFromArguments(arguments, random, reporter)
            null == System.console() -> rollFromStdin(random, reporter)
            else -> rollFromRepl(prompt, random, reporter)
        }
    }
}

private fun rollFromArguments(
    arguments: List<String>,
    random: Random,
    reporter: MainReporter,
) {
    for (argument in arguments)
        if (0 != rollIt(argument, random, reporter)) break
}

private fun rollFromStdin(
    random: Random,
    reporter: MainReporter,
) = rollFromLines(random, reporter) { readLine() }

private typealias ReadLine = () -> String?

internal fun rollFromLines(
    random: Random,
    reporter: MainReporter,
    readLine: ReadLine,
) {
    do {
        val line = readLine()
        when {
            null == line -> return
            line.isEmpty() -> continue
            else -> if (0 != rollIt(line, random, reporter)) return
        }
    } while (true)
}

private fun rollForDemo(
    random: Random,
    reporter: MainReporter,
) {
    for ((expression, _) in demoExpressions) {
        if (reporter is VerboseReporter)
            println("---")
        try {
            rollIt(expression, random, reporter)
        } catch (e: BadExpressionException) {
            err.println(e.message)
        }
    }

    println("DONE")
}

private fun rollIt(
    expression: String,
    random: Random,
    reporter: MainReporter,
): Int {
    val result = roll(expression, random, reporter)
    reporter.display(result)
    return result.parseErrors.size
}

/**
 * Used by both demo and testing.
 * The second value is the expectation given a seed of 1 used in testing.
 */
internal val demoExpressions = arrayOf(
    "d6" to 4,
    " d6" to 4, // whitespace
    "d6 " to 4, // whitespace
    " d6 " to 4, // whitespace
    "D6" to 4,
    "d6x2" to 8,
    "z6" to 3,
    "Z6" to 3,
    "z6x2" to 6,
    "1d1" to 1,
    "1z1" to 0,
    "3d6" to 10,
    "3D6" to 10,
    "1d1" to 1, // check boundary
    "1z1" to 0, // check boundary
    "3d6+1" to 11, // adding adjustment
    "3d6+ 1" to 11, // whitespace
    "3d6 +1" to 11, // whitespace
    "3d6 + 1" to 11, // whitespace
    "3d6x2+1" to 21, // double before adding one
    "3d6-1" to 9, // subtracting adjustment
    "10d3!" to 20,
    "10d3!*2" to 40, // double after exploding
    "10d3!2" to 49,
    "4d6h3" to 10,
    "4d6H3" to 10,
    "4d6l3" to 6,
    "4d6L3" to 6,
    "3d6+2d4" to 17,
    "3d6 +2d4" to 17, // whitespace
    "3d6+ 2d4" to 17, // whitespace
    "3d6 + 2d4" to 17, // whitespace
    "d%" to 66,
    // Constant seed keeps the roll constant, so z% is one less than d%
    "z%" to 65,
    "6d4l5!" to 20,
    "3d3r1h2!" to 10,
    "3d12!10" to 23,
    "100d3r1h99!+100d3r1l99!3-17" to 919,
    "blah" to null,
)
