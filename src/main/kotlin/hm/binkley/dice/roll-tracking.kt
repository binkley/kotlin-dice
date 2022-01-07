package hm.binkley.dice

/**
 * This could have been a `typealias`.  However, Kotlin syntax plays nicer
 * with using a `fun interface` (SAM interface).
 */
fun interface RollReporter {
    fun onRoll(action: RollAction)
}

sealed class RollAction(
    private val roller: Roller,
    val roll: Int,
) {
    val d: Int get() = roller.d
    val n: Int get() = roller.n
    val reroll: Int get() = roller.reroll
    val keep: Int get() = roller.keep
    val explode: Int get() = roller.explode
}

class PlainRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
class PlainReroll(roller: Roller, roll: Int) : RollAction(roller, roll)
class ExplodedRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
class ExplodedReroll(roller: Roller, roll: Int) : RollAction(roller, roll)
class DroppedRoll(roller: Roller, roll: Int) : RollAction(roller, roll)
