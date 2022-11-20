package boolean_formula.basis

import boolean_formula.BooleanFormula

enum class BitValue(val value: Boolean) : BooleanFormula {
    TRUE(true),
    FALSE(false),
}