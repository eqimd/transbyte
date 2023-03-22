package boolean_logic

sealed interface BooleanVariable {
    fun negated(): BooleanVariable

    data class Bit(val bitNumber: Long, val isNegated: Boolean = false) : BooleanVariable {
        override fun toString(): String =
            (if (isNegated) "-" else "") + bitNumber

        override fun negated(): Bit =
            Bit(bitNumber, !isNegated)
    }

    enum class Constant(val value: Boolean) : BooleanVariable {
        TRUE(true) {
            override fun negated(): Constant =
                FALSE
        },
        FALSE(false) {
            override fun negated(): Constant =
                TRUE
        };

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
