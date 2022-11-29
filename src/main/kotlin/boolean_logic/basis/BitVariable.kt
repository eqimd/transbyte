package boolean_logic.basis

import boolean_logic.BooleanFormula

data class BitVariable(val bitNumber: Long, val isNegated: Boolean = false, val value: Boolean? = null) : BooleanFormula {
    override fun toString(): String =
        if (isNegated) "-" else "" + bitNumber
}
