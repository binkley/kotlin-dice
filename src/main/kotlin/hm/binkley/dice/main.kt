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

@Generated // Lie to JaCoCo -- use of exit confuses it
fun main(args: Array<String>): Unit =
    exitProcess(CommandLine(Options()).execute(*args))

private typealias ReadLine = () -> String?

private fun repl(prompt: String) {
    // TODO: This is ugly needing to hack the environment for testing :(
    val term = System.getenv().getOrDefault("TERM", "")
    val dumb = "dumb" == term || term.isEmpty()

    if (dumb) replLogic { readLine() }
    else {
        val isatty = null != System.console()
        val readerPrompt = if (isatty) prompt else null

        fancyRepl(readerPrompt)
    }
}

private fun replLogic(readLine: ReadLine) {
    do {
        val line = readLine()
        when {
            null == line -> return
            line.isEmpty() -> continue
            else -> rollForMain(line)
        }
    } while (true)
}

@Generated // Lie to JaCoCo
private fun fancyRepl(readerPrompt: String?) = TerminalBuilder.builder()
    .name("Dice Roller")
    .build().use { terminal ->
        try {
            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build()
            replLogic { reader.readLine(readerPrompt) }
        } catch (e: UserInterruptException) {
            return // TODO: Should we complain on ^C?
        } catch (e: EndOfFileException) {
            return
        }
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
        rollForMain(expression)

    println("DONE") // Show that bad expression did not throw
}

private var noisy = false
private var random: Random = Random.Default

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

fun rollForMain(expression: String) {
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
    var expressions: List<String> = emptyList()

    override fun call(): Int {
        noisy = verbose
        // TODO: Why does Kotlin require non-null assertion?
        if (null != seed) random = Random(seed!!)

        if (demo) {
            runDemo()
        } else if (expressions.isEmpty()) {
            repl(prompt)
        } else {
            expressions.forEach { rollForMain(it) }
        }

        return 0
    }
}
