package hm.binkley.dice

import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme
import java.lang.System.err
import kotlin.random.Random

val colorScheme = defaultColorScheme(AUTO)

sealed class MainRoller(
    protected val random: Random,
    protected val reporter: MainReporter,
) {
    abstract fun rollAndReport()
}

class CommandLineRoller(
    random: Random,
    reporter: MainReporter,
    private val arguments: List<String>,
) : MainRoller(random, reporter) {
    override fun rollAndReport() =
        arguments.forEach { rollIt(random, reporter, it) }
}

class StdinRoller(
    random: Random,
    reporter: MainReporter,
) : MainRoller(random, reporter) {
    override fun rollAndReport() =
        rollFromLines(random, reporter) { readLine() }
}

class DemoRoller(
    random: Random,
    reporter: MainReporter,
) : MainRoller(random, reporter) {
    override fun rollAndReport() {
        for ((expression, _) in demoExpressions)
            try {
                rollIt(random, reporter, expression)
            } catch (e: DiceException) {
                err.println(colorScheme.errorText(e.message))
            }

        println(colorScheme.string("@|bold DONE|@"))
    }
}

private fun rollIt(
    random: Random,
    reporter: MainReporter,
    expression: String,
) {
    reporter.preRoll()
    val result = roll(expression, random, reporter)
    reporter.display(result)
}

fun rollFromLines(
    random: Random,
    reporter: MainReporter,
    nextLine: () -> String?,
) {
    while (true) {
        val line = nextLine()
        when {
            null == line -> return
            line.isEmpty() -> continue
            else -> rollIt(random, reporter, line)
        }
    }
}
