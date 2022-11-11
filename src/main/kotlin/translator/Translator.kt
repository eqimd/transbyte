package translator

import boolean_formula.BooleanFormula

interface Translator {
    fun translate(): BooleanFormula
}
