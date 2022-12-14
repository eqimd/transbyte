package boolean_logic.base

import boolean_logic.BooleanFormula

data class Negated(val expr: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "¬($expr)"
}
