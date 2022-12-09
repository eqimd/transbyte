package parsed_types.data

import bit_scheduler.BitScheduler
import constants.BitsArray
import parsed_types.ClassSat

sealed interface Variable {
    class BitsArrayWithNumber(val bitsArray: BitsArray, val constant: Number? = null) : Variable

    sealed class ArrayReference(val size: Int? = null) : Variable {
        class ArrayPrimitives(size: Int?, val primitiveSize: Int, bitScheduler: BitScheduler) : ArrayReference(size) {
            val primitives = HashMap<Int, BitsArrayWithNumber>()
            init {
                if (size != null) {
                    for (i in 0 until size) {
                        // TODO add equality to default value
                        primitives[i] = BitsArrayWithNumber(bitScheduler.getAndShift(primitiveSize))
                    }
                }
            }
        }

        class ArrayOfReferences(size: Int?) : ArrayReference(size) {
            val arrays = HashMap<Int, ArrayReference>()
            val references = HashMap<Int, ClassSat>()
        }
    }

    class ClassReference(val classSat: ClassSat) : Variable
}
