package boolean_logic.additional

import boolean_logic.BooleanFormula

data class Xor(val lhs: BooleanFormula, val rhs: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($lhs) ⊕ ($rhs)"
}
