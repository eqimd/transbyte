package translator

import boolean_logic.BooleanFormula

interface Translator {
    fun translate(): BooleanFormula
}
