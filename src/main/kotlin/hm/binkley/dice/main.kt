package hm.binkley.dice

import lombok.Generated
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.ParseError
import org.parboiled.support.ParsingResult
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.lang.System.err
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

internal fun rollFromLines(readLine: ReadLine): Int {
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

private var noisy = false

private fun runDemo(): Int {
    for (expression in demoExpressions) {
        if (noisy) println("---")
        rollIt(expression.first)
    }

    println("DONE") // Show that bad expression did not throw

    return 0
}

private var random: Random = Random.Default
private val NoisyRolling = OnRoll {
    // TODO: Colorize output when using a prompt
    println(
        when (it) {
            is PlainRoll -> "roll(d${it.d}) -> ${it.roll}"
            is PlainReroll -> "reroll(d${it.d}) -> ${it.roll}"
            is ExplodedRoll -> "!roll(d${it.d}: exploding on ${it.explode}) -> ${it.roll}"
            is ExplodedReroll -> "!reroll(d${it.d}: exploding on ${it.explode}) -> ${it.roll}"
            is DroppedRoll -> "drop -> ${it.roll}"
        }
    )
}

private fun rollIt(expression: String): Int {
    return if (noisy) rollNoisily(expression)
    else rollQuietly(expression)
}

sealed interface Displayers {
    fun display(result: ParsingResult<Int>)
    fun display(error: ParseError)

    val ParsingResult<Int>.expression: String
        get() = collectContent(inputBuffer)
    val ParsingResult<Int>.roll: Int get() = resultValue
}

private object PlainDisplayer : Displayers {
    override fun display(result: ParsingResult<Int>) {
        println("${result.expression} ${result.roll}")
    }

    override fun display(error: ParseError) {
        err.println(printParseError(error))
    }
}

private object VerboseDisplayer : Displayers {
    override fun display(result: ParsingResult<Int>) {
        // TODO: Fix mess of "Rolling X" called outside of showing result
        println("RESULT -> ${result.roll}")
    }

    override fun display(error: ParseError) {
        err.println(printParseError(error))
    }
}

private fun rollNoisily(expression: String): Int {
    println("Rolling $expression")
    val result = roll(expression, NoisyRolling, random)
    val verbose = VerboseDisplayer

    return if (!result.hasErrors()) {
        verbose.display(result)
        0
    } else {
        result.parseErrors.forEach {
            verbose.display(it)
        }
        1
    }
}

private fun rollQuietly(expression: String): Int {
    val result = roll(expression, DoNothing, random)
    val plain = PlainDisplayer

    return if (!result.hasErrors()) {
        plain.display(result)
        0
    } else {
        result.parseErrors.forEach {
            plain.display(it)
        }
        1
    }
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
