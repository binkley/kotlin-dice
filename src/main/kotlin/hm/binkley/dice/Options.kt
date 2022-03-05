package hm.binkley.dice

import hm.binkley.dice.Options.Color.auto
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import kotlin.random.Random

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
class Options : Runnable {
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
        description = ["Test terminal for the REPL (INTERNAL)."],
        names = ["--test-repl"],
        hidden = true,
    )
    var testRepl = false

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
        val random = if (null == seed) Random else Random(seed!!)
        val reporter = MainReporter.new(minimum, verbose)

        val roller = when {
            demo -> DemoRoller(random, reporter)
            arguments.isNotEmpty() ->
                CommandLineRoller(random, reporter, arguments)
            // Check --test-repl before checking for pipeline: there is no
            // console in tests
            testRepl -> ReplRoller(random,
                reporter,
                prompt,
                ::testReplReader);
            null == System.console() -> StdinRoller(random, reporter)
            else -> ReplRoller(random, reporter, prompt, ::replReader)
        }

        roller.rollAndReport()
    }
}
