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
            private val primitiveConstants: HashMap<Number, Primitive> = hashMapOf()

            fun create(size: Int, constant: Number? = null): Pair<Primitive, BooleanSystem> {
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

    class ArrayReference<T : VariableSat>(iterable: Iterable<T>) : VariableSat {
        val array: ArrayList<T>

        init {
            array = ArrayList(iterable.iterator().asSequence().toList())
        }

        fun copy(): ArrayReference<T> {
            return ArrayReference(array)
        }
    }

    class ClassReference private constructor(val classSat: ClassSat) : VariableSat
}
