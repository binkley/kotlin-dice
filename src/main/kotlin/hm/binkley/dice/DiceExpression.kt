package hm.binkley.dice

/** Represent the parsed data from a dice expression. */
data class DiceExpression(
    /** The number of die sides, eg, d12 (the 12). */
    val d: Int,
    /**
     * Whether the die is one-based (standard, eg, 1-6) or zero-based (eg,
     * 0-5).
     */
    val dieBase: DieBase,
    /** The number of dice to roll, eg, 3d6 (the three). */
    val n: Int,
    /**
     * Reroll die values this value or lower.  This is not the same as
     * "dropping" dice, as rolling continues to meet [n] (or possibly more
     * dice with [explode]).
     *
     * @todo Syntax and support for comparisons other than less-than-or-equal
     */
    val reroll: Int,
    /**
     * Keep rolls:
     * * If positive, keep the highest values (ie, top N die rolls)
     * * If negative, keep the lowest values (ie, bottom N die rolls)
     * * If zero, keep all values regardless of die roll
     */
    val keep: Int,
    /** Continue rolling more dice while the roll is this value or greater. */
    val explode: Int,
)
