package hm.binkley.dice

import hm.binkley.dice.DiceParser.Companion.dice
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme
import java.lang.System.err
import kotlin.random.Random

val colorScheme = defaultColorScheme(AUTO)!!

sealed class MainRoller(
    random: Random,
    private val reporter: MainReporter,
) {
    private val dice = dice(random, reporter)
    abstract fun rollAndReport()

    protected fun rollFromLines(nextLine: () -> String?) {
        while (true) {
            val expression = nextLine()
            when {
                null == expression -> return
                expression.isEmpty() -> continue
                else -> expression.rollIt()
            }
        }
    }

    protected fun String.rollIt() {
        reporter.preRoll()
        reporter.display(dice.roll(this))
    }
}

class ArgumentRoller(
    random: Random,
    reporter: MainReporter,
    private val arguments: List<String>,
) : MainRoller(random, reporter) {
    override fun rollAndReport() = arguments.forEach { it.rollIt() }
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
        for ((expression, _, description) in demoExpressions) try {
            println(colorScheme.string("@|faint,italic - $description:|@"))
            expression.rollIt()
        } catch (e: DiceException) {
            err.println(colorScheme.errorText(e.message))
        }

        println(colorScheme.string("@|bold DONE|@"))
    }
}
