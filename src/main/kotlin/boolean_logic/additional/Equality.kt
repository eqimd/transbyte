package boolean_logic.additional

import boolean_logic.BooleanFormula
import boolean_logic.basis.BitVariable

class Equality(val lhs: BitVariable, val rhs: BooleanFormula) : BooleanFormula {
    override fun equals(other: Any?): Boolean =
        (other is Equality && ((other.lhs == lhs && other.rhs == rhs) || (other.lhs == rhs && other.rhs == lhs)))

    // Auto generated
    override fun hashCode(): Int {
        var result = lhs.hashCode()
        result = 31 * result + rhs.hashCode()
        return result
    }

    override fun toString(): String =
        "($lhs) ≡ ($rhs)"
}
