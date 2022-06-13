package hm.binkley.dice.rolling

/**
 * The lowest pip on a die.
 * Physical dice are 1-based, _eg_, 1-6 for "d6"; "z dice" are 0-based,
 * _eg_, 0-5 for "z6", equal to one less than the equivalent physical dice.
 */
enum class DieBase(private val value: Int) {
    ZERO(0),
    ONE(1);

    operator fun plus(other: Int) = value + other
    operator fun minus(other: Int) = value - other
    operator fun compareTo(other: Int) = value.compareTo(other)

    /**
     * The least roll of [dieSides]. (`dieSides` is not used, but provides
     * symmetry to [maxRoll].)
     */
    fun minRoll(@Suppress("UNUSED_PARAMETER") dieSides: Int) = value

    /** The greatest roll of [dieSides]. */
    fun maxRoll(dieSides: Int) = value + dieSides - 1
}
