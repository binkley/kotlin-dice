package hm.binkley.dice

import lombok.Generated
import org.fusesource.jansi.AnsiConsole
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

@Generated // Lie to JaCoCo
fun main(args: Array<String>) {
    AnsiConsole.systemInstall()
    val exitCode = try {
        CommandLine(Options()).execute(*args)
    } finally {
        AnsiConsole.systemUninstall()
    }
    // Funny construction so that JAnsi can clean up the terminal before we
    // call exit().  Normally this would be in the "try" block
    exitProcess(exitCode)
}

/**
 * @todo Use JLine for line editing, help, etc
 * @todo No prompt if there is no input
 * @todo No prompt if STDIN is not a terminal, ie, for scripts
 */
private fun readShell(prompt: String) {
    do {
        print(prompt)
        val line = readLine() ?: return
        if (line.isEmpty()) continue
        roll(line)
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
        roll(expression)

    println("DONE") // Show that bad expression did not throw
}

private var noisy = false
private var random: Random = Random.Default

@Generated // Lie to JaCoCo
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

@Generated
fun roll(expression: String) {
    if (noisy) rollNoisily(expression)
    else rollQuietly(expression)
}

@Generated // Lie to JaCoCo
private fun rollNoisily(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = roll(expression, NoisyRolling)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors()) println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}

@Generated // Lie to JaCoCo
private fun rollQuietly(expression: String) {
    val result = roll(expression, DoNothing)
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
@Generated // Lie to JaCoCo -- TODO: tests for CLI
private class Options : Callable<Int> {
    @Option(names = ["--demo"])
    var demo = false

    @Option(names = ["-p", "--prompt"])
    var prompt = "\uD83C\uDFB2 " // Colorful die

    @Option(names = ["-s", "--seed"])
    var seed: Int? = null

    @Option(names = ["-v", "--verbose"])
    var verbose = false

    @Parameters
    var parameters: List<String> = emptyList()

    override fun call(): Int {
        noisy = verbose
        // TODO: Why does Kotlin require non-null assertion?
        if (null != seed) random = Random(seed!!)

        if (demo) {
            runDemo()
        } else if (parameters.isEmpty()) {
            readShell(prompt)
        } else {
            for (expression in parameters) roll(expression)
        }

        return 0
    }
}
