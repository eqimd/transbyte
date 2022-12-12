package boolean_logic.base

import boolean_logic.BooleanFormula

data class BitVariable(val bitNumber: Long, val isNegated: Boolean = false) : BooleanFormula {
    override fun toString(): String =
        if (isNegated) "-" else "" + bitNumber
}
