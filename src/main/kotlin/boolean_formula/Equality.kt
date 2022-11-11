package boolean_formula

class Equality(val lhs: BooleanFormula, val rhs: BooleanFormula) : BooleanFormula {
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
