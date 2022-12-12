package boolean_logic.base

import boolean_logic.BooleanFormula

data class Disjunction(val lhs: BooleanFormula, val rhs: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($lhs) v ($rhs)"
}
