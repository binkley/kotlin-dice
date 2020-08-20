package hm.binkley.dice

fun interface OnRoll {
    fun onRoll(action: RollAction)
}

object DoNothing : OnRoll {
    override fun onRoll(action: RollAction) = Unit
}

sealed class RollAction(
    val n: Int,
    val d: Int,
    val reroll: Int,
    val keep: Int,
    val explode: Int,
    val roll: Int,
) {
    constructor(roller: Roller, roll: Int) : this(
        roller.n,
        roller.d,
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
