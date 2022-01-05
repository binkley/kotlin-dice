package hm.binkley.dice

import kotlin.random.Random

/** @todo Switch from 1 to 123 to harmonize with [MainTest] */
internal fun stableSeedForEachTest() = Random(1L)
