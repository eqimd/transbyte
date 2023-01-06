package parsed_types.data

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import constants.BitsArray
import constants.BooleanSystem
import parsed_types.ClassSat

sealed interface VariableSat {
    class Primitive private constructor(val bitsArray: BitsArray, val constant: Number? = null) : VariableSat {
        companion object {
            fun create(size: Int, constant: Number? = null, bitScheduler: BitScheduler): Pair<Primitive, BooleanSystem> {
                val bitsArray = bitScheduler.getAndShift(size)
                val primitive = Primitive(bitsArray, constant)

                val parseSystem: List<BooleanFormula.Equality>
                if (constant != null) {
                    parseSystem = List(size) { index ->
                        BooleanFormula.Equality(
                            bitsArray[index],
                            BooleanFormula
                                .Variable
                                .Constant
                                .getByBoolean((constant.toLong().shr(index).and(1L) != 0L))
                        )
                    }
                } else {
                    parseSystem = emptyList()
                }

                return Pair(primitive, parseSystem)
            }
        }
    }

    sealed class ArrayReference(val size: Int? = null) : VariableSat {
        class ArrayOfPrimitives private constructor(size: Int?, val primitiveSize: Int) : ArrayReference(size) {
            var primitives = HashMap<Int, Primitive>()

            companion object {
                fun create(size: Int? = null, primitiveSize: Int, bitScheduler: BitScheduler): Pair<ArrayReference, BooleanSystem> {
                    val arrayOfPrimitives = ArrayOfPrimitives(size, primitiveSize)
                    val parseSystem = emptyList<BooleanFormula.Equality>().toMutableList()

                    if (size != null) {
                        for (i in 0 until size) {
                            val (primitive, sys) = Primitive.create(size = primitiveSize, bitScheduler = bitScheduler, constant = 0)
                            parseSystem.addAll(sys)
                            arrayOfPrimitives.primitives[i] = primitive
                        }
                    }

                    return Pair(arrayOfPrimitives, parseSystem)
                }
            }

            fun copy(): ArrayOfPrimitives {
                val copyArray = ArrayOfPrimitives(this.size, this.primitiveSize)
                copyArray.primitives = HashMap(this.primitives)

                return copyArray
            }
        }

        class ArrayOfArrays private constructor(size: Int?) : ArrayReference(size) {
            // TODO
            val arrays = HashMap<Int, ArrayReference>()
        }

        class ArrayOfReferences private constructor(size: Int?) : ArrayReference(size) {
            // TODO
            val references = HashMap<Int, ClassSat>()
        }
    }

    class ClassReference private constructor(val classSat: ClassSat) : VariableSat
}
