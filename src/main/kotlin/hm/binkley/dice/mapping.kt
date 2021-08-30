package hm.binkley.dice

fun <T> Int.mapTo(mapping: (Int) -> T?) = mapping(this)
    ?: throw IllegalStateException("Missing `$this` in $mapping")
