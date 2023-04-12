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
    fun saveInAag(printStream: PrintStream = System.out) {
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

    fun saveInDimacs(printStream: PrintStream = System.out) {
        val maximumVariableIndex = system.maxBy { it.bit.bitNumber }.bit.bitNumber
        val outSize = output?.size ?: 0

        printStream.println("c Input bits:")
        printStream.println("c " + input.joinToString(separator = " ") { it.bitNumber.toString() })
        printStream.println("c Output bits:")
        printStream.println("c " + (output?.joinToString(separator = " ") { (it.bitNumber).toString() + maximumVariableIndex } ?: ""))

        val clauses = emptyList<String>().toMutableList()

        for (eq in system) {
            when (eq.conjFirst) {
                is BooleanVariable.Bit -> {
                    clauses.add("-${eq.bit.bitNumber} ${if (eq.conjFirst.isNegated) "-" else ""}${eq.conjFirst.bitNumber} 0")
                    when (eq.conjSecond) {
                        is BooleanVariable.Bit -> {
                            clauses.add("-${eq.bit.bitNumber} ${if (eq.conjSecond.isNegated) "-" else ""}${eq.conjSecond.bitNumber} 0")
                            clauses.add(
                                "${eq.bit.bitNumber} ${if (eq.conjFirst.isNegated) "" else "-"}${eq.conjFirst.bitNumber} " +
                                    "${if (eq.conjSecond.isNegated) "" else "-"}${eq.conjSecond.bitNumber} 0"
                            )
                        }
                        BooleanVariable.Constant.TRUE -> {
                            clauses.add("${eq.bit.bitNumber} ${if (eq.conjFirst.isNegated) "" else "-"}${eq.conjFirst.bitNumber} 0")
                        }
                        BooleanVariable.Constant.FALSE -> {
                            clauses.add("-${eq.bit.bitNumber} 0")
                        }
                    }
                }
                BooleanVariable.Constant.TRUE -> {
                    when (eq.conjSecond) {
                        is BooleanVariable.Bit -> {
                            clauses.add("-${eq.bit.bitNumber} ${if (eq.conjSecond.isNegated) "-" else ""}${eq.conjSecond.bitNumber} 0")
                            clauses.add("${eq.bit.bitNumber} ${if (eq.conjSecond.isNegated) "" else "-"}${eq.conjSecond.bitNumber} 0")
                        }
                        BooleanVariable.Constant.TRUE -> {
                            clauses.add("${eq.bit.bitNumber} 0")
                        }
                        BooleanVariable.Constant.FALSE -> {
                            clauses.add("-${eq.bit.bitNumber} 0")
                        }
                    }
                }
                BooleanVariable.Constant.FALSE -> {
                    clauses.add("-${eq.bit.bitNumber} 0")
                    if (eq.conjSecond !is BooleanVariable.Constant) {
                        eq.conjSecond as BooleanVariable.Bit
                        clauses.add("-${eq.bit.bitNumber} ${if (eq.conjSecond.isNegated) "-" else ""}${eq.conjSecond.bitNumber} 0")
                    }
                }
            }
        }

        // Add variables so output indexes will be greatest
        for (i in 1..outSize) {
            val outBit = output!![i - 1]
            clauses.add("-${maximumVariableIndex + i} ${if (outBit.isNegated) "-" else ""}${outBit.bitNumber} 0")
            clauses.add("${maximumVariableIndex + i} ${if (outBit.isNegated) "" else "-"}${output[i - 1].bitNumber} 0")
        }

        printStream.println("p cnf ${maximumVariableIndex + outSize} ${clauses.size}")
        for (c in clauses) {
            printStream.println(c)
        }
    }
}
