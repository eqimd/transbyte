package boolean_logic

class Equality(
    val bit: BooleanVariable.Bit,
    val conjFirst: BooleanVariable,
    val conjSecond: BooleanVariable = BooleanVariable.Constant.TRUE
) {
    override fun toString(): String =
        "$bit ≡ $conjFirst · $conjSecond"
}
