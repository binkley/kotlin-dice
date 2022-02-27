package hm.binkley.dice

import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme
import java.lang.System.err
import kotlin.random.Random

val colorScheme = defaultColorScheme(AUTO)

fun rollFromCommandLine(
    arguments: List<String>,
    random: Random,
    reporter: MainReporter,
) = arguments.forEach { rollIt(it, random, reporter) }

fun rollFromStdin(
    random: Random,
    reporter: MainReporter,
) = rollFromLines(random, reporter) { readLine() }

fun rollForDemo(
    random: Random,
    reporter: MainReporter,
) {
    for ((expression, _) in demoExpressions)
        try {
            rollIt(expression, random, reporter)
        } catch (e: DiceException) {
            err.println(colorScheme.errorText(e.message))
        }

    println(colorScheme.string("@|bold DONE|@"))
}

private fun rollIt(
    expression: String,
    random: Random,
    reporter: MainReporter,
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
            else -> rollIt(line, random, reporter)
        }
    }
}
