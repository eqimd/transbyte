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

        val latches = emptyList<String>().toMutableList()
        val andGates = emptyList<String>().toMutableList()

        for (eq in system) {
            val aigBit = eq.bit.bitNumber * 2

            if (eq.conjSecond == null) {
                val secondBit = when (val conj = eq.conjFirst) {
                    is BooleanFormula.Variable.Bit -> {
                        conj.bitNumber * 2 + conj.isNegated.toInt()
                    }
                    is BooleanFormula.Variable.Constant -> {
                        conj.value.toInt()
                    }
                }

                latches.add("$aigBit $secondBit")
            } else {
                val firstBit = when (val conj = eq.conjFirst) {
                    is BooleanFormula.Variable.Bit -> {
                        conj.bitNumber * 2 + conj.isNegated.toInt()
                    }
                    is BooleanFormula.Variable.Constant -> {
                        conj.value.toInt()
                    }
                }

                val secondBit = when (val conj = eq.conjSecond!!) {
                    is BooleanFormula.Variable.Bit -> {
                        conj.bitNumber * 2 + conj.isNegated.toInt()
                    }
                    is BooleanFormula.Variable.Constant -> {
                        conj.value.toInt()
                    }
                }

                andGates.add("$aigBit $firstBit $secondBit")
            }
        }

        val numberOfLatches = latches.size
        val numberOfAndGates = andGates.size

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

        for (latch in latches) {
            printStream.println(latch)
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

        for (andGate in andGates) {
            printStream.println(andGate)
        }
    }
}