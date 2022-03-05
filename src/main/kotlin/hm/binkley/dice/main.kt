package hm.binkley.dice

import hm.binkley.dice.Options.Color.auto
import org.jline.reader.UserInterruptException
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
const val COLORFUL_DIE_PROMPT = "\uD83C\uDFB2 "

fun main(args: Array<String>) {
    val options = Options()
    val processSpecialOptions = IExecutionStrategy special@{ parseResult ->
        // TODO: How to get Picocli to do these for me?
        if (options.copyright) {
            options.javaClass
                .classLoader
                .getResourceAsStream("META-INF/LICENSE")
                .copyTo(System.out)
            return@special 0
        }

        options.color.install()

        RunLast().execute(parseResult)
    }

    exitProcess(
        CommandLine(options)
            .setExecutionExceptionHandler(exceptionHandling)
            .setExecutionStrategy(processSpecialOptions)
            // Use last value for repeated options (ie, `--color`)
            .setOverwrittenOptionsAllowed(true)
            .execute(*args)
    )
}

val exceptionHandling = IExecutionExceptionHandler { ex, commandLine, _ ->
    when (ex) {
        // User-friendly error message
        is DiceException -> {
            if (commandLine.getCommand<Options>().debug)
                commandLine.err.println(colorScheme.richStackTraceString(ex))
            else {
                val interactive = null != System.console()
                // GNU standards prefix errors with program name to aid in
                // debugging failed scripts, etc.
                val prefix = if (interactive) "" else "$PROGRAM_NAME: "
                commandLine.err.println(
                    colorScheme.errorText(prefix + ex.message)
                )
            }
            commandLine.commandSpec.exitCodeOnExecutionException() // 1
        }
        // Special case for the REPL - shells return 130 on SIGINT
        is UserInterruptException -> 130
        // Unknown exceptions fall back to Picolo default handling
        else -> throw ex
    }
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
     Start the interactive dice rolling prompt (REPL)
  @|bold roll|@ <@|italic expression(s)|@>
     Print result of dice expression(s), and exit.
  echo @|italic <expression(s)>|@ | @|bold roll|@
     Print result of dice expression(s) read from STDIN, and exit.

@|bold,underline Output examples:|@
  @|bold roll --seed=1 2d4 2d4|@ (normal)
     2d4 @|fg_green,bold 4|@
     2d4 @|fg_green,bold 7|@
  @|bold roll --seed=1 --verbose 2d4 2d4|@ (verbose)
     ---
     @|faint,italic roll(d4) -> 1|@
     @|faint,italic roll(d4) -> 3|@
     @|bold 2d4|@ -> @|fg_green,bold 4|@
     ---
     @|faint,italic roll(d4) -> 4|@
     @|faint,italic roll(d4) -> 3|@
     @|bold 2d4|@ -> @|fg_green,bold 7|@

@|bold,underline Error messages:|@
  @|italic Incomplete dice expression '<EXPRESSION>'|@
     More characters were expected at the end of EXPRESSION.
  @|italic Unexpected '<CHAR>' (at position <POS>) in dice expression '<EXPRESSION>'|@
     CHAR was not expected in EXPRESSION at position POS (starting from 1).

@|bold,underline Exit codes:|@
  @|bold   0|@ - Successful completion
  @|bold   1|@ - Bad dice expression
  @|bold   2|@ - Bad program usage
  @|bold 130|@ - REPL interrupted (SIGINT)"""
    ],
)
internal class Options : Runnable {
    /**
     * Arguments to the `--color` flag based on GNU standards.
     * The enum name is identical to the argument and case-sensitive.
     * Example: `--color=always`.
     */
    @Suppress("EnumEntryName", "unused")
    enum class Color(private val ansi: Boolean?) {
        // Force color
        always(true),
        yes(true),
        force(true),

        // Guess for color
        auto(null),
        tty(null),
        `if-tty`(null),

        // Disable color
        never(false),
        no(false),
        none(false),
        ;

        fun install() {
            when (ansi) {
                null -> System.clearProperty("picocli.ansi")
                else -> System.setProperty("picocli.ansi", "$ansi")
            }
        }
    }

    @Option(
        description = [
            "Choose color output (\${COMPLETION-CANDIDATES})",
            "If specified without a WHEN, it uses '\${FALLBACK-VALUE}'",
            "Without this option, color is used if at a tty"
        ],
        names = ["-C", "--color"],
        paramLabel = "WHEN",
        arity = "0..1",
        fallbackValue = "always",
    )
    var color = auto

    @Option(
        description = ["Verbose and with developer output (INTERNAL)."],
        names = ["--debug"],
        hidden = true,
    )
    var debug = false

    @Option(
        description = ["Print copyright and exit."],
        names = ["--copyright"],
    )
    var copyright = false

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
        description = ["Change the interactive prompt from '$COLORFUL_DIE_PROMPT'."],
        names = ["-p", "--prompt"],
        paramLabel = "PROMPT",
    )
    var prompt = COLORFUL_DIE_PROMPT // Colorful die

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
        description = [
            "Dice expressions to roll",
            "If provided no expressions, prompt user interactively"
        ],
        paramLabel = "EXPRESSION",
    )
    var arguments: List<String> = emptyList()

    override fun run() {
        if (debug) verbose = true

        // TODO: Why does Kotlin require non-null assertion?
        val random = if (null == seed) Random.Default else Random(seed!!)
        val reporter = MainReporter.new(minimum, verbose)

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
    "1d1" to 1, // check bounds
    "1z1" to 0, // check bounds
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
