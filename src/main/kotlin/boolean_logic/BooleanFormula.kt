package boolean_logic

sealed interface BooleanFormula {

    sealed interface Variable : BooleanFormula {
        data class Bit(val bitNumber: Long, val isNegated: Boolean = false) : Variable {
            override fun toString(): String =
                (if (isNegated) "-" else "") + bitNumber

            fun negated(): Bit =
                Bit(bitNumber, !isNegated)
        }

        enum class Constant(val value: Boolean) : Variable {
            TRUE(true),
            FALSE(false);

            companion object {
                fun getByBoolean(value: Boolean): Constant =
                    when {
                        value -> {
                            TRUE
                        }
                        else -> {
                            FALSE
                        }
                    }
            }
        }
    }

    class Equality(val bit: Variable.Bit, conjFirst: Variable, vararg conjAnother: Variable) : BooleanFormula {

        val conjuncts: List<Variable>

        init {
            this.conjuncts = listOf(conjFirst, *conjAnother)
        }
        override fun toString(): String =
            "$bit ≡ ${conjuncts.joinToString(separator = " · ")}"
    }
}
