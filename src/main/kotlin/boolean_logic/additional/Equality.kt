package boolean_logic.additional

import boolean_logic.BooleanFormula
import boolean_logic.base.BitVariable

data class Equality(val lhs: BitVariable, val rhs: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($lhs) ≡ ($rhs)"
}
