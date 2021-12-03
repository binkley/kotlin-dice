package hm.binkley.dice

import lombok.Generated
import org.fusesource.jansi.AnsiConsole
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.parboiled.errors.ErrorUtils.printParseError
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.lang.System.err
import java.lang.System.out
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Generated // Lie to JaCoCo
fun main(args: Array<String>) {
    // Funny construction so that JAnsi can clean up the terminal before we
    // call exit()
    AnsiConsole.systemInstall()
    val exitCode = try {
        val terminal = TerminalBuilder.builder().build()
        val parser = DefaultParser()
        CommandLine(Options()).execute(*args)
    } finally {
        AnsiConsole.systemUninstall()
    }
    exitProcess(exitCode)
}

private fun runDemo() {
    roll("D6")
    roll("z6")
    roll("3d6")
    roll("3z6")
    roll("3d6+1")
    roll("3d6-1")
    roll("10d3!")
    roll("10d3!2")
    roll("4d6h3")
    roll("4d6l3")
    roll("3d6+2d4")
    roll("d%")
    roll("6d4l5!")
    roll("3d12r1h2!11")
    roll("blah")
    println("DONE") // Show that bad expression did not throw
}

private var noisy = false

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
    if (!result.hasErrors())
        println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}

@Generated // Lie to JaCoCo
private fun rollQuietly(expression: String) {
    val result = roll(expression, DoNothing)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors())
        println("$expression ${result.resultValue}")
    err.flush()
    out.flush()
}

@Command(
    name = "dice",
    mixinStandardHelpOptions = true,
    version = ["dice 0-SNAPSHOT"]
)
private class Options : Callable<Int> {
    @Option(names = ["--demo"])
    var demo = false

    @Option(names = ["-v", "--verbose"])
    var verbose = false

    @Parameters
    var parameters: List<String> = emptyList()

    override fun call(): Int {
        noisy = verbose

        if (demo) runDemo()

        for (expression in parameters)
            roll(expression)

        return 0
    }
}
