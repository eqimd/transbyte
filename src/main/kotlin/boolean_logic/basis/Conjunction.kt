package boolean_logic.basis

import boolean_logic.BooleanFormula

data class Conjunction(val lhs: BooleanFormula, val rhs: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($lhs) · ($rhs)"
}
