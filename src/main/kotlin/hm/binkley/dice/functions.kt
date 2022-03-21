package hm.binkley.dice

import picocli.CommandLine.Help.Ansi

fun isInteractive() = null != System.console()
fun isColor() = Ansi.AUTO.enabled()
