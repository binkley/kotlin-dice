package hm.binkley.dice

/**
 * Arguments to the `--color` option based on GNU standards.
 * The enum name is identical to the argument and case-sensitive.
 * Example: `--color=always`.
 */
@Suppress("EnumEntryName")
enum class ColorOption(private val ansi: Boolean?) {
    // Force color
    always(true),
    yes(always.ansi),
    force(always.ansi),
    // Guess for color
    auto(null),
    tty(auto.ansi),
    `if-tty`(auto.ansi),
    // Disable color
    never(false),
    no(never.ansi),
    none(never.ansi);

    fun install() {
        when (ansi) {
            null -> System.clearProperty("picocli.ansi")
            else -> System.setProperty("picocli.ansi", "$ansi")
        }
    }
}
