package boolean_formula

class Maj(val x: BooleanFormula, val y: BooleanFormula, val z: BooleanFormula) : BooleanFormula {
    override fun toString(): String =
        "($x)·($y) v ($x)·($z) v ($y)·($z)"
}