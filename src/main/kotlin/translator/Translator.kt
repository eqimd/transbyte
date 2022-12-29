package translator

import parsed_types.data.EncodingCircuit
import parsed_types.data.Variable

interface Translator {
    fun translate(className: String, methodDescription: String): EncodingCircuit

    fun translate(className: String, methodDescription: String, vararg args: Variable): EncodingCircuit
}
