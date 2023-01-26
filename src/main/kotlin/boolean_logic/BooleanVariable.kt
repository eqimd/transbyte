package boolean_logic

sealed interface BooleanVariable {
    data class Bit(val bitNumber: Long, val isNegated: Boolean = false) : BooleanVariable {
        override fun toString(): String =
            (if (isNegated) "-" else "") + bitNumber

        fun negated(): Bit =
            Bit(bitNumber, !isNegated)
    }

    enum class Constant(val value: Boolean) : BooleanVariable {
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
