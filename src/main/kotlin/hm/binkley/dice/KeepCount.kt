package hm.binkley.dice

import java.util.Objects.hash

sealed class KeepCount(protected val value: Int) {
    override fun toString() = "${this::class.simpleName}($value)"

    override fun equals(other: Any?) = this === other ||
        other is KeepCount &&
        javaClass == other.javaClass &&
        value == other.value

    override fun hashCode() = hash(javaClass, value)

    /** Splits sorted list of results into those to keep and those to drop. */
    protected abstract fun partition(other: List<Int>):
        Pair<List<Int>, List<Int>>

    companion object {
        fun List<Int>.keep(count: KeepCount) = count.partition(this)
    }
}

/**
 * Keeps the highest dice rolls.
 */
class KeepHigh(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            other.size - value <= index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

/**
 * Keeps the lower middle dice rolls.
 * When there are even/odd numbers of dice, and even/odd numbers to keep:
 * - Even / even -- middle low and high are the same
 * - Even /odd -- picks up an extra low roll
 * - Odd / even -- picks up an extra low roll
 * - Odd / odd -- middle low and high are the same
 */
class KeepMiddleLow(value: Int) : KeepMiddle(value) {
    override fun splitKeep(listEven: Boolean, keepEven: Boolean) =
        if (listEven && keepEven) value / 2 else value / 2 + 1
}

/**
 * Keeps the upper middle dice rolls.
 * When there are even/odd numbers of dice, and even/odd numbers to keep:
 * - Even / even -- middle low and high are the same
 * - Even /odd -- picks up an extra high roll
 * - Odd / even -- picks up an extra high roll
 * - Odd / odd -- middle low and high are the same
 */
class KeepMiddleHigh(value: Int) : KeepMiddle(value) {
    override fun splitKeep(listEven: Boolean, keepEven: Boolean) =
        if (!listEven && !keepEven) value / 2 + 1 else value / 2
}

abstract class KeepMiddle(
    value: Int,
) : KeepCount(value) {
    protected abstract fun splitKeep(
        listEven: Boolean,
        keepEven: Boolean,
    ): Int

    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        if (0 == value || other.isEmpty())
            return emptyList<Int>() to other

        val listEven = 0 == other.size % 2
        val keepEven = 0 == value % 2

        val lowerSize =
            if (listEven) other.size / 2
            else other.size / 2 + 1
        val lowerKeep = splitKeep(listEven, keepEven)
        val upperKeep = value - lowerKeep

        val (lower, upper) = other.keep(KeepLow(lowerSize))
        val (lowerKept, lowerDropped) = lower.keep(KeepHigh(lowerKeep))
        val (upperKept, upperDropped) = upper.keep(KeepLow(upperKeep))

        return (lowerKept + upperKept) to (lowerDropped + upperDropped)
    }
}

/**
 * Keeps the lowest dice rolls.
 */
class KeepLow(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            value > index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

private fun <T> List<IndexedValue<T>>.withoutIndex() = map { (_, it) -> it }
