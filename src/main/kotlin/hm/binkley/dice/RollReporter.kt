package hm.binkley.dice

/**
 * Tracing for each individual die rolled.
 * For example, the dice expression "2d20h1", given a RNG seeded with 1,
 * yields these traces:
 *  - [PlainRoll] of "d20" with result 6
 *  - [PlainRoll] of "d20" with result 17
 *  - [DroppedRoll] of "d20" with result 6
 */
fun interface RollReporter {
    fun onRoll(dice: RolledDice)
}

/** Typing for why a die was rolled. */
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
