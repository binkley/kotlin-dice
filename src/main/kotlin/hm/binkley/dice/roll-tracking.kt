package hm.binkley.dice

/**
 * This could have been a `typealias`.  However, Kotlin syntax plays nicer
 * with using a `fun interface` (SAM interface).
 */
fun interface RollReporter {
    fun onRoll(action: RollAction)
}

sealed class RollAction(
    private val expression: DiceExpression,
    val roll: Int,
) {
    val dieBase: DieBase get() = expression.dieBase
    val dieSides: Int get() = expression.dieSides
    val diceCount: Int get() = expression.diceCount
    val reroll: Int get() = expression.rerollLow
    val keep: Int get() = expression.keep
    val explode: Int get() = expression.explode
    val multiply: Int get() = expression.multiply
}

class PlainRoll(expression: DiceExpression, roll: Int) :
    RollAction(expression, roll)

class PlainReroll(expression: DiceExpression, roll: Int) :
    RollAction(expression, roll)

class ExplodedRoll(expression: DiceExpression, roll: Int) :
    RollAction(expression, roll)

class ExplodedReroll(expression: DiceExpression, roll: Int) :
    RollAction(expression, roll)

class DroppedRoll(expression: DiceExpression, roll: Int) :
    RollAction(expression, roll)
