package hm.binkley.dice

import kotlin.random.Random

internal const val TESTING_SEED = 1L

internal fun stableSeedForEachTest() = Random(TESTING_SEED)
