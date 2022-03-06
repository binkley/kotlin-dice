package hm.binkley.dice

import java.util.Objects.hash

sealed class KeepCount(protected val value: Int) {
    override fun toString() = "${this::class.simpleName}($value)"

    override fun equals(other: Any?) = this === other ||
            other is KeepCount &&
            javaClass == other.javaClass &&
            value == other.value

    override fun hashCode() = hash(javaClass, value)

    abstract fun partition(other: List<Int>): Pair<List<Int>, List<Int>>
}

class KeepHigh(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            other.size - value <= index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

class KeepLow(value: Int) : KeepCount(value) {
    override fun partition(other: List<Int>): Pair<List<Int>, List<Int>> {
        val (kept, dropped) = other.withIndex().partition { (index, _) ->
            value > index
        }

        return kept.withoutIndex() to dropped.withoutIndex()
    }
}

private fun <T> List<IndexedValue<T>>.withoutIndex() = map { (_, it) -> it }
