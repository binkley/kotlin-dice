package hm.binkley.dice

import org.parboiled.errors.ErrorUtils
import java.lang.System.err
import java.lang.System.out

fun main() {
    verbose = true

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
    val result = DiceParser.roll(expression)
    result.parseErrors.forEach {
        err.println(ErrorUtils.printParseError(it))
    }
    if (!result.hasErrors())
        println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}
