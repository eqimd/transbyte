package boolean_formula.additional

import boolean_formula.BooleanFormula

class Maj(val x: BooleanFormula, val y: BooleanFormula, val z: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($x)·($y) v ($x)·($z) v ($y)·($z)"
}
