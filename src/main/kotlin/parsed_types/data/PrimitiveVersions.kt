package parsed_types.data

import boolean_logic.BooleanVariable
import constants.BooleanSystem
import operation_parser.OperationParser

class PrimitiveVersions {
    private val _versionsMap: MutableMap<Number, BooleanVariable.Bit> = mutableMapOf()
    val versionsMap: Map<Number, BooleanVariable.Bit> = _versionsMap

    fun add(number: Number, bit: BooleanVariable.Bit): BooleanSystem {
        return if (_versionsMap.containsKey(number)) {
            val (newBit, sys) = OperationParser.parseDisjunctionBits(bit, _versionsMap[number]!!)
            _versionsMap[number] = newBit

            sys
        } else {
            _versionsMap[number] = bit

            emptyList()
        }
    }

    fun clear() {
        _versionsMap.clear()
    }
}
