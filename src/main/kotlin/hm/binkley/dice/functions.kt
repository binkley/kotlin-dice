package hm.binkley.dice

import picocli.CommandLine.Help.Ansi

fun isColor() = Ansi.AUTO.enabled()
fun isInteractive() = null != System.console()
