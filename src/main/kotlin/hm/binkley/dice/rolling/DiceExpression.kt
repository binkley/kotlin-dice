package hm.binkley.dice.rolling

interface DiceExpression {
    /** The original dice expression input. */
    val expression: String

    /**
     * Whether the die is one-based (standard, eg, 1-6) or zero-based (eg,
     * 0-5).
     * The default is one-based.
     */
    val dieBase: DieBase

    /**
     * The number of die sides, eg, d12 (the 12).
     * There is no default: parsing fails.
     */
    val dieSides: Int

    /**
     * The number of dice to roll, eg, 3d6 (the three).
     * The default is to roll one die.
     */
    val diceCount: Int

    /**
     * Reroll low rolls.
     * This is not the same as "dropping" dice, as rolling continues to
     * meet [diceCount] (or possibly more dice with [explodeHigh]).
     * The default is to reroll no dice.
     *
     * @todo Syntax and support for comparisons other than less-than-or-equal
     */
    val rerollLow: Int

    /**
     * Which rolls to keep.
     * The default is to keep all rolls.
     */
    val keepCount: KeepCount

    /**
     * Continue rolling more dice while the roll is this value or greater.
     * The default is to explode no rolls.
     */
    val explodeHigh: Int

    /**
     * Multiply the dice rolls, or the final dice result.
     * The default is to multiply by one (no change).
     */
    val multiply: Int
}
