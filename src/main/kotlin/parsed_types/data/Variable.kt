package parsed_types.data

import constants.BitsArray
import parsed_types.ClassSat

sealed interface Variable {
    class BitsArrayWithNumber(val bitsArray: BitsArray, val constant: Number? = null) : Variable

    sealed class ArrayReference(val size: Int? = null) : Variable {
        class ArrayPrimitives(size: Int?, primitiveSize: Int) : ArrayReference(size) {
            val primitives = HashMap<Int, BitsArrayWithNumber>()
        }

        class ArrayOfReferences(size: Int?) : ArrayReference(size) {
            val arrays = HashMap<Int, ArrayReference>()
            val references = HashMap<Int, ClassSat>()
        }
    }

    class ClassReference(val classSat: ClassSat) : Variable
}
