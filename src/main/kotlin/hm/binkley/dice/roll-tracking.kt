package hm.binkley.dice

/**
 * This could have been a `typealias`.  However, Kotlin syntax plays nicer
 * with using a `fun interface` (SAM interface).
 */
fun interface OnRoll {
    fun onRoll(action: RollAction)
}

/**
 * *Not* an `enum class`.  The subtypes need to be specific to values
 * passed in the constructors.
 */
sealed class RollAction(
    val d: Int,
    val n: Int,
    val reroll: Int,
    val keep: Int,
    val explode: Int,
    val roll: Int,
) {
    constructor(roller: Roller, roll: Int) : this(
        roller.d,
        roller.n,
        roller.reroll,
        roller.keep,
        roller.explode,
        roll,
    )
}

class PlainRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
class PlainReroll(roller: Roller, roll: Int) : RollAction(roller, roll)
class ExplodedRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
class ExplodedReroll(roller: Roller, roll: Int) : RollAction(roller, roll)
class DroppedRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
