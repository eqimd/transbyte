package parsed_types.data

import constants.BooleanSystem

data class EncodingCircuit(val input: List<Variable>?, val output: Variable?, val system: BooleanSystem)
