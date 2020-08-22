package hm.binkley.dice

import lombok.Generated
import org.parboiled.errors.ErrorUtils.printParseError
import java.lang.System.err
import java.lang.System.out

@Generated // Lie to JaCoCo
fun main() {
    rollNoisily("D6")
    rollNoisily("3d6")
    rollNoisily("3d6+1")
    rollNoisily("3d6-1")
    rollNoisily("10d3!")
    rollNoisily("10d3!2")
    rollNoisily("4d6h3")
    rollNoisily("4d6l3")
    rollNoisily("3d6+2d4")
    rollNoisily("d%")
    rollNoisily("6d4l5!")
    rollNoisily("3d12r1h2!11")
    rollNoisily("blah")
    println("DONE") // Show that bad expression did not throw
}

@Generated // Lie to JaCoCo
private val NoisyRolling = OnRoll {
    println(when (it) {
        is PlainRoll -> "roll(d${it.d}) -> ${it.roll}"
        is PlainReroll -> "reroll(d${it.d}) -> ${it.roll}"
        is ExplodedRoll -> "!roll(d${it.d}) -> ${it.roll}"
        is ExplodedReroll -> "!reroll(d${it.d}) -> ${it.roll}"
        is DroppedRoll -> "drop -> ${it.roll}"
    })
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
