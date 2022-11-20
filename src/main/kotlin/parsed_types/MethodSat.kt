package parsed_types

import bit_number_scheduler.BitsScheduler
import boolean_logic.BooleanFormula
import boolean_logic.additional.Equality
import boolean_logic.additional.Maj
import boolean_logic.additional.Xor
import boolean_logic.basis.Conjunction
import boolean_logic.basis.Disjunction
import constants.BitsArray
import constants.BooleanSystem
import constants.Constants.INT_BITS
import constants.Constants.LONG_BITS
import exception.MethodParseException
import exception.UnsupportedParseInstructionException
import extension.bitsSize
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.ILOAD
import org.apache.bcel.generic.IMUL
import org.apache.bcel.generic.INVOKESTATIC
import org.apache.bcel.generic.IRETURN
import org.apache.bcel.generic.LADD
import org.apache.bcel.generic.LMUL
import org.apache.bcel.generic.MethodGen
import org.apache.bcel.generic.RETURN

class MethodSat(
    private val clazz: JavaClass,
    private val classSat: ClassSat,
    private val method: Method,
    cpGen: ConstantPoolGen,
    private val bitScheduler: BitsScheduler,
) {
    private val methodGen = MethodGen(method, clazz.className, cpGen)

    val name: String = methodGen.name

    fun parse(argsBitFields: List<BitsArray>): BooleanSystem {
        checkParseErrors(argsBitFields)

        val locals = parseArgsToLocals(argsBitFields)

        // Stack for primitive variables
        val stackVariables = ArrayDeque<BitsArray>()

        // Stack for references
        val stackReferences = ArrayDeque<ClassSat>()

        val system = emptyList<List<BooleanFormula>>().toMutableList()

        for (instrHandle in methodGen.instructionList) {
            when (val instruction = instrHandle.instruction) {
                is ILOAD -> {
                    stackVariables.addLast(locals[instruction.index])
                }
                is IADD, is LADD -> {
                    val a = stackVariables.removeLast()
                    val b = stackVariables.removeLast()
                    val varSize = if (instruction is IADD) INT_BITS else LONG_BITS
                    val (c, parseSystem) = parseIADD(a, b, varSize)
                    stackVariables.addLast(c)
                    system.add(parseSystem)
                }
                is IMUL, is LMUL -> {
                    val a = stackVariables.removeLast()
                    val b = stackVariables.removeLast()
                    val varSize = if (instruction is IMUL) INT_BITS else LONG_BITS
                    val (c, parseSystem) = parseIMUL(a, b, varSize)
                    stackVariables.addLast(c)
                    system.add(parseSystem)
                }
                is INVOKESTATIC -> {
                    // TODO parse() method should return reference or bit array`
                    val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                    invokedMethod.parse(emptyList())
                }
                is IRETURN -> {
                }
                is RETURN -> {
                }
                else -> {
                    throw UnsupportedParseInstructionException("Can't parse $instruction: instruction is not supported")
                }
            }
        }

        return system
    }

    private fun checkParseErrors(argsBitFields: List<BitsArray>) {
        if (argsBitFields.size != methodGen.argumentNames.size) {
            throw MethodParseException(
                "Number of arguments, given to .parse(...), is not same that is for" +
                    "`${clazz.className}` : `$method`"
            )
        }
        for ((index, pair) in (argsBitFields zip methodGen.argumentTypes).withIndex()) {
            if (pair.first.size != pair.second.bitsSize) {
                throw MethodParseException(
                    "Size ${pair.first.size} of argument with index $index" +
                        "is not equal to ${pair.second.bitsSize}," +
                        "which is size of type ${pair.second}"
                )
            }
        }
    }

    private fun parseArgsToLocals(argsBitFields: List<BitsArray>): Array<BitsArray> {
        val locals = Array<BitsArray>(methodGen.maxLocals) { emptyArray() }
        for ((index, elem) in argsBitFields.withIndex()) {
            locals[index] = elem
        }

        return locals
    }

    private fun parseIADD(a: BitsArray, b: BitsArray, varSize: Int = INT_BITS): Pair<BitsArray, MutableList<BooleanFormula>> {
        // TODO should it consider overflowing?

        val system = emptyList<BooleanFormula>().toMutableList()

        // c = a + b
        val c = bitScheduler.getAndShift(varSize + 1)

        val c0 = Equality(
            c[0],
            Xor(
                a[0],
                b[0]
            )
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(varSize)
        var pPrev = pArr[0]
        val p0 = Equality(
            pPrev,
            Conjunction(
                a[0],
                b[0]
            )
        )
        system.add(p0)

        for (i in 1 until varSize) {
            val maj = Maj(
                a[i],
                b[i],
                pPrev
            )
            val xor = Xor(
                Xor(
                    a[i],
                    b[i]
                ),
                pPrev
            )
            pPrev = pArr[i]
            val pI = Equality(
                pPrev,
                maj
            )
            val cI = Equality(
                c[i],
                xor
            )
            system.add(pI)
            system.add(cI)
        }

        val cLast = Equality(
            c[c.size - 1],
            pPrev
        )
        system.add(cLast)

        return Pair(c, system)
    }

    private fun parseIMUL(a: BitsArray, b: BitsArray, varSize: Int = INT_BITS): Pair<BitsArray, MutableList<BooleanFormula>> {
        val system = emptyList<BooleanFormula>().toMutableList()

        // c = a*b
        val c = bitScheduler.getAndShift(2 * varSize)

        val temp = bitScheduler.getAndShift(varSize * varSize)
        val tempMult = Array(varSize) { i ->
            Array(varSize) { j -> temp[i * varSize + j] }
        }

        val sumRes = bitScheduler.getAndShift(varSize * varSize)
        val sumResMult = Array(varSize) { i ->
            Array(varSize) { j -> sumRes[i * varSize + j] }
        }

        val carry = bitScheduler.getAndShift(varSize * varSize)
        val carryMult = Array(varSize) { i ->
            Array(varSize) { j -> carry[i * varSize + j] }
        }

        for (i in 0 until varSize) {
            for (j in 0 until varSize) {
                system.add(
                    Equality(
                        tempMult[i][j],
                        Conjunction(a[i], b[j])
                    )
                )
            }
        }

        for (i in 0 until varSize - 1) {
            system.add(
                Equality(
                    sumResMult[0][i],
                    Xor(tempMult[0][i + 1], tempMult[i + 1][0])
                )
            )

            system.add(
                Equality(
                    carryMult[0][i],
                    Conjunction(tempMult[0][i + 1], tempMult[i + 1][0])
                )
            )
        }

        for (i in 0 until varSize - 1) {
            for (j in 0 until varSize - 1) {
                system.add(
                    Equality(
                        sumResMult[i + 1][j],
                        Xor(
                            carryMult[i][j],
                            Xor(
                                sumResMult[i][j + 1],
                                tempMult[j + 1][i + 1]
                            )
                        )
                    )
                )

                system.add(
                    Equality(
                        carryMult[i + 1][j],
                        Disjunction(
                            Conjunction(tempMult[j + 1][i + 1], carryMult[i][j]),
                            Disjunction(
                                Conjunction(tempMult[j + 1][i + 1], sumResMult[i][j + 1]),
                                Conjunction(carryMult[i][j], sumResMult[i][j + 1])
                            )
                        )
                    )
                )
            }
        }

        system.add(
            Equality(c[0], tempMult[0][0])
        )

        for (i in 1 until varSize) {
            system.add(
                Equality(c[i], sumResMult[i - 1][0])
            )
        }

        for (i in 0 until varSize - 1) {
            system.add(
                Equality(c[i + varSize], sumResMult[varSize - 1][i])
            )
        }

        system.add(
            Equality(c[2 * varSize - 1], carryMult[varSize - 1][varSize - 2])
        )

        return Pair(c, system)
    }
}
