# Kotlin Dice Parser

A dice expression has these parts:

- 1 or more roll expressions, added/subtracted together
- An optional adjustment, added/subtracted at the end

The smallest roll expression is just a die type, eg, `d6` meaning roll 1
6-sided die.  See "Examples", below.

## Dice expression syntax

```
[N]'d'D['r'R]['h'K|'l'K][!|!Z][+EXP|-EXP...][+A|-A]
```
- N - number of dice, default 1
- D - sides on the die, or '%' for percentile dice
- R - reroll dice this or lower, eg, reroll 1s
- K - keep highest ('h') or ('l') lowest rolls
- Z - "explode" on die face or greater, default is to explode on max die face
- EXP - add/subtract more dice expressions
- A - add/subtract this fixed amount to the result

## Examples

- `d6` -- roll 1 6-sided die
- `2d%+1` -- roll percentile dice 2 times, sum, and add 1 to the result
- `3d6r1!` -- roll 3 6-sided dice, rerolling 1s, "explode" on 6s
- `3d6r1!5` -- roll 3 6-sided dice, rerolling 1s, "explode" on 5s or 6s
- `2d4+2d6h1` -- roll 2 4-sided dice, sum; roll 2 6-sided dice keeping the
  highest 1; add both results

## Code conventions

As each top-level part of a roll expression (eg, die type) parse, a numeric
value is pushed onto a stack provided by the parser.  By the end of the
roll expression, the stack contains from top down:

- Adjustment, or 0 if none specified
- Explosion limit, or "die type + 1" if none specified
- Dice to keep, or "roll count" if none specified; a positive number is
  keep highest, a negative number is keep lowest
- Reroll value, or 0 if none specified; rolls of this value or lower are
  rerolled
- Die type, ie, number of die sides
- Roll count, or 1 if none specified; ie, number of dice to roll

Evaluating and individual roll expression clears the stack, leaving only:

- Running total of previous results

## References

* [roll](https://github.com/matteocorti/roll#examples)
* [_Dice Syntax_](https://rollem.rocks/syntax/)
* [_Dice notation_](https://en.wikipedia.org/wiki/Dice_notation)
