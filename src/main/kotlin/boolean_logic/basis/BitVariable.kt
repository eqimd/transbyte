package boolean_logic.basis

import boolean_logic.BooleanFormula

class BitVariable(val bitNumber: Long, val isNegated: Boolean = false) : BooleanFormula {
    override fun equals(other: Any?): Boolean =
        (other is BitVariable && other.bitNumber == bitNumber && other.isNegated == isNegated)

    // Auto generated
    override fun hashCode(): Int {
        var result = bitNumber.hashCode()
        result = 31 * result + isNegated.hashCode()
        return result
    }

    override fun toString(): String =
        if (isNegated) "-" else "" + bitNumber
}
