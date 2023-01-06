package parsed_types.data

import boolean_logic.BooleanFormula
import constants.BooleanSystem
import extension.toInt
import java.io.PrintStream

data class EncodingCircuit(val input: List<VariableSat>, val output: VariableSat?, val system: BooleanSystem) {
    fun saveInAigerFormat(printStream: PrintStream = System.out) {
        val maximumVariableIndex = system.maxBy { it.bit.bitNumber }.bit
        val numberOfInputs = input.sumOf {
            when (it) {
                is VariableSat.Primitive -> {
                    it.bitsArray.size
                }
                is VariableSat.ArrayReference.ArrayOfPrimitives -> {
                    it.primitives.values.sumOf { prim -> prim.bitsArray.size }
                }
                else -> {
                    TODO("Only primitives and arrays of primitives supported right now")
                }
            }
        }
        // TODO what is this?
        val numberOfLatches = 0
        val numberOfOutputs = when (output) {
            null -> {
                0
            }
            is VariableSat.Primitive -> {
                output.bitsArray.size
            }
            is VariableSat.ArrayReference.ArrayOfPrimitives -> {
                output.primitives.values.sumOf { it.bitsArray.size }
            }
            else -> {
                TODO("Only primitives and arrays of primitives supported right now")
            }
        }

        val numberOfAndGates = system.size

        printStream.println(
            "aag $maximumVariableIndex $numberOfInputs $numberOfLatches $numberOfOutputs $numberOfAndGates"
        )

        for (variable in input) {
            when (variable) {
                is VariableSat.Primitive -> {
                    variable.bitsArray.forEach {
                        printStream.println(it.bitNumber * 2)
                    }
                }
                is VariableSat.ArrayReference.ArrayOfPrimitives -> {
                    variable.primitives.values.forEach { prim ->
                        prim.bitsArray.forEach {
                            printStream.println(it.bitNumber * 2)
                        }
                    }
                }
                else -> {
                    TODO("Only primitives and arrays of primitives supported right now")
                }
            }
        }

        when (output) {
            is VariableSat.Primitive -> {
                output.bitsArray.forEach {
                    printStream.println(it.bitNumber * 2)
                }
            }
            is VariableSat.ArrayReference.ArrayOfPrimitives -> {
                output.primitives.values.forEach { prim ->
                    prim.bitsArray.forEach {
                        printStream.println(it.bitNumber * 2)
                    }
                }
            }
            else -> {
                TODO("Only primitives and arrays of primitives supported right now")
            }
        }

        for (eq in system) {
            val aigBit = eq.bit.bitNumber * 2
            printStream.print("$aigBit ")

            eq.conjuncts.forEach {
                when (it) {
                    is BooleanFormula.Variable.Bit -> {
                        val printBit = it.bitNumber * 2 + it.isNegated.toInt()
                        printStream.print("$printBit ")
                    }
                    is BooleanFormula.Variable.Constant -> {
                        printStream.print("${it.value.toInt()} ")
                    }
                }
            }
            printStream.println()
        }
    }
}
