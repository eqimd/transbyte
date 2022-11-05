package boolean_formula

class Bit(val bitNumber: Long, val isNegated: Boolean) : BooleanFormula {
    override fun equals(other: Any?): Boolean =
        (other is Bit && other.bitNumber == bitNumber && other.isNegated == isNegated)

    // Auto generated
    override fun hashCode(): Int {
        var result = bitNumber.hashCode()
        result = 31 * result + isNegated.hashCode()
        return result
    }

    override fun toString(): String =
        if (isNegated) "-" else "" + bitNumber
}