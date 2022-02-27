package hm.binkley.dice

/**
 * This could have been a `typealias`.  However, Kotlin syntax plays nicer
 * with using a `fun interface` (SAM interface).
 */
fun interface RollReporter {
    fun onRoll(dice: RolledDice)
}

sealed class RolledDice(
    expression: DiceExpression,
    val roll: Int,
) : DiceExpression by expression

class PlainRoll(expression: DiceExpression, roll: Int) :
    RolledDice(expression, roll)

class PlainReroll(expression: DiceExpression, roll: Int) :
    RolledDice(expression, roll)

class ExplodedRoll(expression: DiceExpression, roll: Int) :
    RolledDice(expression, roll)

class ExplodedReroll(expression: DiceExpression, roll: Int) :
    RolledDice(expression, roll)

class DroppedRoll(expression: DiceExpression, roll: Int) :
    RolledDice(expression, roll)
