package hm.binkley.dice

import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme
import java.lang.System.err
import kotlin.random.Random

val colorScheme = defaultColorScheme(AUTO)

sealed class MainRoller(
    private val random: Random,
    private val reporter: MainReporter,
) {
    abstract fun rollAndReport()

    protected fun rollFromLines(nextLine: () -> String?) {
        while (true) {
            val line = nextLine()
            when {
                null == line -> return
                line.isEmpty() -> continue
                else -> rollIt(line)
            }
        }
    }

    protected fun rollIt(expression: String) {
        reporter.preRoll()
        val result = roll(expression, random, reporter)
        reporter.display(result)
    }
}

class CommandLineRoller(
    random: Random,
    reporter: MainReporter,
    private val arguments: List<String>,
) : MainRoller(random, reporter) {
    override fun rollAndReport() = arguments.forEach { rollIt(it) }
}

class StdinRoller(
    random: Random,
    reporter: MainReporter,
) : MainRoller(random, reporter) {
    override fun rollAndReport() = rollFromLines { readLine() }
}

class DemoRoller(
    random: Random,
    reporter: MainReporter,
) : MainRoller(random, reporter) {
    override fun rollAndReport() {
        for ((expression, _) in demoExpressions)
            try {
                rollIt(expression)
            } catch (e: DiceException) {
                err.println(colorScheme.errorText(e.message))
            }

        println(colorScheme.string("@|bold DONE|@"))
    }
}
