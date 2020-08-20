package hm.binkley.dice

fun interface OnRoll {
    fun onRoll(action: RollAction)
}

object DoNothing : OnRoll {
    // TODO: Change from String to a sealed class with specific details: let
    //       the callback decide how to present the information
    override fun onRoll(action: RollAction) = Unit
}

sealed class RollAction(
    val n: Int,
    val d: Int,
    val reroll: Int,
    val keep: Int,
    val explode: Int,
) {
    constructor(roller: Roller) : this(
        roller.n,
        roller.d,
        roller.reroll,
        roller.keep,
        roller.explode,
    )
}

class PlainRoll(
    roller: Roller,
    val roll: Int,
) : RollAction(roller)

class ExplodedRoll(
    roller: Roller,
    val roll: Int,
) : RollAction(roller)

class PlainReroll(
    roller: Roller,
    val roll: Int,
) : RollAction(roller)

class ExplodedReroll(
    roller: Roller,
    val roll: Int,
) : RollAction(roller)

class DroppedRoll(
    roller: Roller,
    val roll: Int,
) : RollAction(roller)
