package hm.binkley.dice

import hm.binkley.dice.DiceParser.Companion.rollLoudly
import org.parboiled.errors.ErrorUtils.printParseError
import java.lang.System.err
import java.lang.System.out

fun main() {
    showRolls("3d6")
    showRolls("3d6+1")
    showRolls("3d6-1")
    showRolls("10d3!")
    showRolls("10d3!2")
    showRolls("4d6h3")
    showRolls("4d6l3")
    showRolls("3d6+2d4")
    showRolls("d%")
    showRolls("6d4l5!")
    showRolls("3d3r1h2!")
    showRolls("blah")
    showRolls("d6")
}

private fun showRolls(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = rollLoudly(expression)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors())
        println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}
