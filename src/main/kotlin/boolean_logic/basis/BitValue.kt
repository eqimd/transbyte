package boolean_logic.basis

import boolean_logic.BooleanFormula

enum class BitValue(val value: Boolean) : BooleanFormula {
    TRUE(true),
    FALSE(false);

    companion object {
        fun getByBoolean(value: Boolean): BitValue =
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
