package parsed_types.data

import boolean_logic.BooleanVariable

data class PrimitiveVersion(val primitive: VariableSat.Primitive, val conditionBit: BooleanVariable.Bit)
