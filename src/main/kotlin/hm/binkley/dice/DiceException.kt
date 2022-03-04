package hm.binkley.dice

import org.parboiled.errors.ErrorUtils.printParseError
import org.parboiled.errors.InvalidInputError
import org.parboiled.errors.ParseError
import org.parboiled.support.Chars

sealed class DiceException(message: String) : Exception(message)

class BadExpressionException(errors: List<ParseError>) :
    DiceException(
        errors.joinToString("\n") {
            if (it is InvalidInputError) oneLinerFor(it)
            else printParseError(it)
        }
    )

private fun oneLinerFor(error: InvalidInputError): String {
    val at = error.startIndex
    with(error.inputBuffer) {
        val ch = charAt(at)
        val position = getPosition(at)
        val where = position.column
        val expression = extractLine(position.line)
        return if (Chars.EOI == ch)
            "Incomplete dice expression '$expression'"
        else
            "Unexpected '$ch' (at position $where) in dice expression '$expression'"
    }
}

class RollTooLowException(
    minimum: Int,
    roll: Int,
) : DiceException(
    "Roll result $roll is below the minimum result of $minimum"
)
