package hm.binkley.dice

import lombok.Generated
import org.parboiled.errors.ErrorUtils.printParseError
import java.lang.System.err
import java.lang.System.out

@Generated // Lie to JaCoCo
fun main() {
    showRolls("D6")
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
    showRolls("3d12r1h2!11")
    showRolls("blah")
    println("DONE") // So that bad expression did not throw
}

@Generated // Lie to JaCoCo
private object ShowRolls : OnRoll {
    override fun onRoll(message: String) = println(message)
}

@Generated // Lie to JaCoCo
private fun showRolls(expression: String) {
    println("---")
    println("Rolling $expression")
    val result = roll(expression, ShowRolls)
    result.parseErrors.forEach {
        err.println(printParseError(it))
    }
    if (!result.hasErrors())
        println("RESULT -> ${result.resultValue}")
    err.flush()
    out.flush()
}
