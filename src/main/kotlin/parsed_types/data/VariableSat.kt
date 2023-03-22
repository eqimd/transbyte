package parsed_types.data

import boolean_logic.BooleanVariable
import boolean_logic.Equality
import constants.BitsArray
import constants.BooleanSystem
import constants.GlobalSettings
import operation_parser.OperationParser
import parsed_types.ClassSat

sealed interface VariableSat {
    class Primitive private constructor(val bitsArray: BitsArray, val constant: Number? = null) : VariableSat {

        private val _versionsMap: MutableMap<Number, BooleanVariable.Bit> = mutableMapOf()
        val versions: Map<Number, BooleanVariable.Bit> = _versionsMap

        companion object {
            fun create(size: Int, constant: Number? = null): Pair<Primitive, BooleanSystem> {
                val primitiveConstants = GlobalSettings.primitiveConstants
                if (constant != null && primitiveConstants.containsKey(constant)) {
                    val primitive = Primitive(
                        primitiveConstants[constant]!!.bitsArray.take(size).toTypedArray(),
                        constant
                    )

                    return Pair(primitive, emptyList())
                }

                val bitScheduler = GlobalSettings.bitScheduler

                val bitsArray = bitScheduler.getAndShift(size)
                val primitive = Primitive(bitsArray, constant)

                val parseSystem: List<Equality> = if (constant != null) {
                    primitiveConstants[constant] = primitive

                    List(size) { index ->
                        Equality(
                            bitsArray[index],
                            BooleanVariable.Constant
                                .getByBoolean((constant.toLong().shr(index).and(1L) != 0L))
                        )
                    }
                } else {
                    emptyList()
                }

                return Pair(primitive, parseSystem)
            }
        }

        fun addVersion(number: Number, bit: BooleanVariable.Bit): BooleanSystem {
            return if (_versionsMap.containsKey(number)) {
                val (newBit, sys) = OperationParser.parseDisjunctionBits(bit, _versionsMap[number]!!)
                _versionsMap[number] = newBit

                sys
            } else {
                _versionsMap[number] = bit

                emptyList()
            }
        }

        fun clearVersions() {
            _versionsMap.clear()
        }
    }

    sealed class ArrayReference(val size: Int? = null) : VariableSat {
        class ArrayOfPrimitives private constructor(size: Int?, val primitiveSize: Int) : ArrayReference(size) {
            var primitives = HashMap<Int, Primitive>()

            companion object {
                fun create(
                    size: Int? = null,
                    primitiveSize: Int,
                    constant: Number? = null
                ): Pair<ArrayReference, BooleanSystem> {
                    val arrayOfPrimitives = ArrayOfPrimitives(size, primitiveSize)
                    val parseSystem = emptyList<Equality>().toMutableList()

                    if (size != null) {
                        for (i in 0 until size) {
                            val (primitive, sys) = Primitive.create(size = primitiveSize, constant = constant)
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
