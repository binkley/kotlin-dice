package hm.binkley.dice

/**
 * The lowest pip on a die.
 * Physical dice are 1-based, _eg_, 1-6 for "d6"; "z dice" are 0-based,
 * _eg_, 0-5 for "z6", equal to one less than the equivalent physical dice.
 */
enum class DieBase {
    ZERO {
        override val value: Int = 0
    },
    ONE {
        override val value: Int = 1
    };

    abstract val value: Int
}
