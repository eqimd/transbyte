package extension

import constants.Constants.BOOLEAN_BITS
import constants.Constants.BYTE_BITS
import constants.Constants.CHAR_BITS
import constants.Constants.DOUBLE_BITS
import constants.Constants.FLOAT_BITS
import constants.Constants.INT_BITS
import constants.Constants.LONG_BITS
import constants.Constants.SHORT_BITS
import exception.TypeSizeException
import org.apache.bcel.generic.Type

val Type.bitsSize: Int
    get() =
        when (this) {
            Type.BOOLEAN -> BOOLEAN_BITS
            Type.INT -> INT_BITS
            Type.SHORT -> SHORT_BITS
            Type.BYTE -> BYTE_BITS
            Type.LONG -> LONG_BITS
            Type.DOUBLE -> DOUBLE_BITS
            Type.FLOAT -> FLOAT_BITS
            Type.CHAR -> CHAR_BITS
            else -> throw TypeSizeException("Can't get bits size of type $this")
        }
