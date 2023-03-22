package parsed_types.data

import boolean_logic.BooleanVariable
import constants.BooleanSystem
import extension.toInt
import java.io.PrintStream

data class EncodingCircuit(
    val input: List<BooleanVariable.Bit>,
    val output: List<BooleanVariable.Bit>?,
    val system: BooleanSystem
) {
    fun saveInAigerFormat(printStream: PrintStream = System.out) {
        val maximumVariableIndex = system.maxBy { it.bit.bitNumber }.bit

        val numberOfInputs = input.size

        val numberOfOutputs = when (output) {
            null -> {
                0
            }
            else -> {
                output.size
            }
        }

        val numberOfLatches = 0
        val numberOfAndGates = system.size

        printStream.println(
            "aag $maximumVariableIndex $numberOfInputs $numberOfLatches $numberOfOutputs $numberOfAndGates"
        )

        for (variable in input) {
            printStream.println(variable.bitNumber * 2)
        }

        if (output != null) {
            for (variable in output) {
                printStream.println(variable.bitNumber * 2)
            }
        }

        for (eq in system) {
            val aigBit = eq.bit.bitNumber * 2

            val firstBit = when (val conj = eq.conjFirst) {
                is BooleanVariable.Bit -> {
                    conj.bitNumber * 2 + conj.isNegated.toInt()
                }
                is BooleanVariable.Constant -> {
                    conj.value.toInt()
                }
            }

            val secondBit = when (val conj = eq.conjSecond) {
                is BooleanVariable.Bit -> {
                    conj.bitNumber * 2 + conj.isNegated.toInt()
                }
                is BooleanVariable.Constant -> {
                    conj.value.toInt()
                }
            }
            printStream.println("$aigBit $firstBit $secondBit")
        }
    }
}
