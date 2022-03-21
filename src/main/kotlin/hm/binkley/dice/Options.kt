package hm.binkley.dice

import hm.binkley.dice.command.ClearScreenCommand
import hm.binkley.dice.command.HistoryCommand
import hm.binkley.dice.command.OptionsCommand
import org.jline.console.SystemRegistry
import org.jline.terminal.Terminal
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Option.NULL_VALUE
import picocli.CommandLine.Parameters
import picocli.CommandLine.Spec
import kotlin.random.Random

@Command(
    // TODO: New REPL only
    commandListHeading = "%n@|bold,underline Commands:|@%n",
    description = ["Roll dice expressions."],
    descriptionHeading = "%n@|bold,underline Description:|@%n",
    footer = [
        """
@|bold,underline Input modes:|@
  @|bold roll|@
     Run the REPL.
  @|bold roll|@ <@|italic expression(s)|@>
     Show roll results of dice expression(s) and exit.
  echo @|italic <expression(s)>|@ | @|bold roll|@
     Show roll result of dice expression(s) read from STDIN and exit.

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

@|bold,underline Files:|@
  @|italic ~/.roll_history|@
     This file preserves input history across runs of the REPL.

@|bold,underline Error messages:|@
  @|italic Incomplete dice expression '<EXPRESSION>'|@
     More characters were expected at the end of EXPRESSION.
  @|italic Unexpected '<CHAR>' (at position <POS>) in dice expression '<EXPRESSION>'|@
     CHAR was not expected in EXPRESSION at position POS (starting from 1).
  @|italic Result <ROLL> is below the minimum result of <NUMBER>|@
     ROLL is too low for the NUMBER in the --minimum option.
  @|italic Exploding on <NUMBER> will never finish in dice expression '<EXPRESSION>'|@
     NUMBER is too low for the number of sides on the die.
  @|italic History disabled because of the --no-history option|@
     Read a history command ('!' first character) but option set for no history.

@|bold,underline Exit codes:|@
  @|bold   0|@   Successful completion
  @|bold   1|@   Bad dice expression
  @|bold   2|@   Bad program usage
  @|bold 130|@   REPL interrupted (SIGINT)
        """
    ],
    mixinStandardHelpOptions = true,
    name = PROGRAM_NAME,
    optionListHeading = "%n@|bold,underline Options:|@%n",
    parameterListHeading = "%n@|bold,underline Parameters:|@%n",
    showAtFileInUsageHelp = true,
    showEndOfOptionsDelimiterInUsageHelp = true,
    synopsisHeading = "@|bold,underline Usage:|@%n",
    version = ["dice 0-SNAPSHOT"],
    subcommands = [
        ClearScreenCommand::class,
        HistoryCommand::class,
        OptionsCommand::class,
    ]
)
class Options : Runnable {
    @Spec
    lateinit var commandSpec: CommandSpec
    lateinit var terminal: Terminal
    lateinit var commandLine: CommandLine
    lateinit var systemRegistry: SystemRegistry
    lateinit var lineReader: org.jline.reader.LineReader

    // TODO: Temporary while cutting over to main main
    @Option(
        description = [
            "Use the new REPL (EXPERIMENTAL)."
        ],
        names = ["--new-repl"],
        hidden = true,
    )
    var newRepl = false

    @Option(
        defaultValue = "auto",
        description = [
            "Choose color output (\${COMPLETION-CANDIDATES}).",
            "Default with no option is '\${DEFAULT-VALUE}'.",
            "Default with option but no WHEN is '\${FALLBACK-VALUE}'.",
        ],
        names = ["-C", "--color"],
        paramLabel = "WHEN",
        arity = "0..1",
        fallbackValue = "always",
    )
    var color = ColorOption.auto

    @Option(
        description = [
            "Verbose and with developer output (INTERNAL)."
        ],
        names = ["--debug"],
        hidden = true,
    )
    var debug = false

    @Option(
        description = [
            "Show the copyright and exit."
        ],
        names = ["--copyright"],
    )
    var copyright = false

    @Option(
        description = [
            "Run the demo and exit."
        ],
        names = ["--demo"],
    )
    var demo = false

    @Option(
        description = [
            "Do not save history from the REPL."
        ],
        names = ["--no-history"],
    )
    var history = true

    @Option(
        description = [
            "Fail roll results below MINIMUM.",
            "Default with no option is no minimum.",
        ],
        names = ["-m", "--minimum"],
        paramLabel = "MINIMUM",
    )
    var minimum = Int.MIN_VALUE

    @Option(
        defaultValue = DIE_PROMPT, // Colorful die
        description = [
            "Change the REPL prompt from '\${DEFAULT-VALUE}'."
        ],
        names = ["-P", "--prompt"],
        paramLabel = "PROMPT",
    )
    var prompt = DIE_PROMPT

    @Option(
        defaultValue = NULL_VALUE,
        description = [
            "Fix RNG seed to SEED for repeatable roll results."
        ],
        names = ["-s", "--seed"],
        paramLabel = "SEED",
    )
    var seed: Int? = null

    @Option(
        description = [
            "Test terminal for the REPL (INTERNAL)."
        ],
        hidden = true,
        names = ["--test-repl"],
    )
    var testRepl = false

    @Option(
        description = [
            "Show die rolls as they happens."
        ],
        names = ["-v", "--verbose"],
    )
    var verbose = false

    @Parameters(
        description = [
            "Dice expressions to roll."
        ],
        paramLabel = "EXPRESSION(s)",
    )
    var arguments: List<String> = emptyList()

    override fun run() {
        maybeWarnNewRepl()

        // Handle copyright first so we can return 0 quickly
        if (copyright) {
            printCopyright()
            return
        }

        if (debug) verbose = true

        pickRoller().rollAndReport()
    }

    private fun maybeWarnNewRepl() {
        if (newRepl) commandLine.err.println(
            colorScheme.errorText(
                "WARNING: the new REPL is EXPERIMENTAL".maybeGnuPrefix()
            )
        )
    }

    private fun printCopyright() {
        javaClass.classLoader
            .getResourceAsStream("META-INF/LICENSE")!!
            .copyTo(System.out)
    }

    private fun pickRoller(): MainRoller {
        // TODO: Why does Kotlin require non-null assertion?
        val random = if (null == seed) Random else Random(seed!!)
        val reporter = MainReporter.new(minimum, verbose)

        return when {
            demo -> DemoRoller(random, reporter)
            arguments.isNotEmpty() ->
                ArgumentRoller(random, reporter, arguments)
            isInteractive() || testRepl -> pickRepl(random, reporter)
            else -> StdinRoller(random, reporter)
        }
    }
}
