package hm.binkley.dice

import hm.binkley.dice.DieBase.ONE
import hm.binkley.dice.DieBase.ZERO
import lombok.Generated
import org.parboiled.buffers.InputBufferUtils.collectContent
import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.InvalidInputError
import org.parboiled.errors.ParseError
import org.parboiled.support.Chars.EOI
import org.parboiled.support.ParsingResult
import picocli.CommandLine.Help.Ansi.AUTO
import picocli.CommandLine.Help.defaultColorScheme

internal open class DiceException(message: String) : Exception(message)

internal class BadExpressionException(errors: List<ParseError>) :
    DiceException(errors.joinToString("\n") {
        if (it is InvalidInputError) oneLinerFor(it)
        else printParseError(it)
    })

private fun oneLinerFor(error: InvalidInputError): String {
    val at = error.startIndex
    with(error.inputBuffer) {
        val char = charAt(at)
        val position = getPosition(at)
        val where = position.column
        val expression = extractLine(position.line)
        return if (EOI == char)
            "Unexpected end in '$expression'"
        else
            "Unexpected '$char' (at position $where) in '$expression'"
    }
}

internal class RollTooLowException(
    minimum: Int,
    roll: Int,
) : DiceException(
    "Roll result $roll is below the minimum result of $minimum"
)

internal fun selectMainReporter(
    minimum: Int,
    verbose: Boolean,
): MainReporter =
    if (verbose) VerboseReporter(minimum)
    else PlainReporter(minimum)

sealed class MainReporter(private val minimum: Int) : RollReporter {
    fun display(result: ParsingResult<Int>) = with(result) {
        if (hasErrors())
            throw BadExpressionException(parseErrors)
        if (minimum > resultValue)
            throw RollTooLowException(minimum, resultValue)

        val expression = collectContent(inputBuffer).trim()
        println(toDisplay(expression, resultValue))
    }

    abstract fun preRoll()

    protected abstract fun toDisplay(expression: String, roll: Int): String
}

internal class PlainReporter(
    minimum: Int
) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = Unit
    override fun preRoll() = Unit

    override fun toDisplay(expression: String, roll: Int) =
        "$expression @|bold,green $roll|@"
}

@Generated // Lie to Lombok
internal class VerboseReporter(minimum: Int) : MainReporter(minimum) {
    override fun onRoll(action: RollAction) = verboseRolling(action)
    override fun preRoll() = println("---")

    override fun toDisplay(expression: String, roll: Int) =
        "@|bold $expression|@ -> @|bold,green $roll|@"
}

private fun verboseRolling(action: RollAction) = with(action) {
    val die = when (dieBase) {
        ONE -> "d$dieSides"
        ZERO -> "z$dieSides"
    }
    val message = when (this) {
        is PlainRoll -> "roll($die) -> $roll"
        is PlainReroll -> "reroll($die) -> $roll"
        is ExplodedRoll -> "!roll($die: exploded $explodeHigh) -> $roll"
        is ExplodedReroll ->
            "!reroll($die: exploded $explodeHigh) -> $roll"
        is DroppedRoll -> "drop($die) -> $roll"
    }

    println(defaultColorScheme(AUTO).string("@|faint $message|@"))
}

private fun println(message: Any?) = kotlin.io.println(
    if (message is String) AUTO.text(message) else message
)
