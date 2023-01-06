package translator

import parsed_types.data.EncodingCircuit
import parsed_types.data.VariableSat

interface Translator {
    fun translate(className: String, methodDescription: String, vararg args: VariableSat): EncodingCircuit
}
