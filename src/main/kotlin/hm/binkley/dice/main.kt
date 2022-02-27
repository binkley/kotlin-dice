package hm.binkley.dice

import hm.binkley.dice.Options.Color.auto
import lombok.Generated
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.IExecutionStrategy
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.CommandLine.RunLast
import kotlin.random.Random
import kotlin.system.exitProcess

const val PROGRAM_NAME = "roll"

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
            options.color.install()
            RunLast().execute(parseResult)
        }

    exitProcess(
        CommandLine(options)
            .setExecutionExceptionHandler(simpleExceptionReporting)
            .setExecutionStrategy(forceColorIfRequested)
            .setOverwrittenOptionsAllowed(true) // Dups use last occurrence
            .execute(*args)
    )
}

@Command(
    name = PROGRAM_NAME,
    description = ["Roll dice expressions."],
    mixinStandardHelpOptions = true,
    version = ["dice 0-SNAPSHOT"],
    synopsisHeading = "@|bold,underline Usage:|@%n",
    descriptionHeading = "%n@|bold,underline Description:|@%n",
    optionListHeading = "%n@|bold,underline Options:|@%n",
    parameterListHeading = "%n@|bold,underline Parameters:|@%n",
    footer = [
        """
@|bold,underline Input modes:|@
  @|bold roll|@
     Start the interactive dice rolling prompt.
  @|bold roll|@ <@|italic expression(s)|@>
     Print result of dice expression(s), and exit.
  echo @|italic <expression(s)>|@ | @|bold roll|@
     Print result of dice expression(s) read from STDIN, and exit.

@|bold,underline Output examples:|@
  @|bold roll --seed=1 2d4 2d4|@ (normal)
     2d4 @|bold,green 4|@
     2d4 @|bold,green 7|@
  @|bold roll --seed=1 --verbose 2d4 2d4|@ (verbose)
     ---
     @|faint,italic roll(d4) -> 1|@
     @|faint,italic roll(d4) -> 3|@
     @|bold 2d4|@ -> @|bold,green 4|@
     ---
     @|faint,italic roll(d4) -> 4|@
     @|faint,italic roll(d4) -> 3|@
     @|bold 2d4|@ -> @|bold,green 7|@

@|bold,underline Exit codes:|@
  @|bold 0|@ - Successful completion
  @|bold 1|@ - Bad dice expression
  @|bold 2|@ - Bad program usage"""
    ],
)
@Generated // Lie to JaCoCo
private class Options : Runnable {
    @Suppress("EnumEntryName", "unused")
    enum class Color(private val ansi: String?) {
        always("true"),
        auto(null),
        never("false");

        fun install() {
            when (ansi) {
                null -> System.clearProperty("picocli.ansi")
                else -> System.setProperty("picocli.ansi", ansi)
            }
        }
    }

    @Option(
        description = ["Choose color output (\${COMPLETION-CANDIDATES})",
            "If specified without a WHEN, it uses '\${FALLBACK-VALUE}'",
            "Without this option, color is used if at a tty"],
        names = ["-C", "--color"],
        paramLabel = "WHEN",
        arity = "0..1",
        fallbackValue = "always",
    )
    var color = auto

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
            "If provided no expressions, prompt user interactively"],
        paramLabel = "EXPRESSION",
    )
    var arguments: List<String> = emptyList()

    override fun run() {
        // TODO: Why does Kotlin require non-null assertion?
        val random = if (null == seed) Random.Default else Random(seed!!)
        val reporter = selectMainReporter(minimum, verbose)

        val roller = when {
            demo -> DemoRoller(random, reporter)
            arguments.isNotEmpty() ->
                CommandLineRoller(random, reporter, arguments)
            null == System.console() -> StdinRoller(random, reporter)
            else -> ReplRoller(random, reporter, prompt)
        }

        roller.rollAndReport()
    }
}

/**
 * Used by both demo and testing.
 * The second value is the expectation given a seed of 1 used in testing.
 */
val demoExpressions = arrayOf(
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
    "10d3!x2" to 40, // synonym for times
    "10d3!X2" to 40, // synonym for times
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
