package parsed_sat

import bit_number_scheduler.BitsScheduler
import boolean_formula.BooleanFormula
import boolean_formula.Conjunction
import boolean_formula.Disjunction
import boolean_formula.Equality
import boolean_formula.Maj
import boolean_formula.Xor
import constants.BitsArray
import constants.Constants.INT_BITS
import exception.MethodParseException
import exception.UnsupportedParseInstructionException
import extension.bitsSize
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.ILOAD
import org.apache.bcel.generic.IMUL
import org.apache.bcel.generic.IRETURN
import org.apache.bcel.generic.MethodGen

class MethodSat(
    private val clazz: JavaClass,
    private val method: Method,
    private val cpGen: ConstantPoolGen,
    private val bitScheduler: BitsScheduler,
) {
    private val methodGen = MethodGen(method, clazz.className, cpGen)

    val name = methodGen.name

    fun parse(argsBitFields: List<BitsArray>): List<BooleanFormula> {
        checkParseErrors(argsBitFields)

        val locals = parseArgsToLocals(argsBitFields)
        val stack = ArrayDeque<BitsArray>()

        val system = emptyList<BooleanFormula>().toMutableList()

        for (instrHandle in methodGen.instructionList) {
            when (val instruction = instrHandle.instruction) {
                is ILOAD -> {
                    stack.addLast(locals[instruction.index])
                }
                is IADD -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()
                    val c = parseIADD(a, b, system)
                    stack.addLast(c)
                }
                is IMUL -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()
                    val c = parseIMUL(a, b, system)
                    stack.addLast(c)
                }
                is IRETURN -> {
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

    private fun parseIADD(a: BitsArray, b: BitsArray, system: MutableList<BooleanFormula>): BitsArray {
        // TODO should it consider overflowing?

        // c = a + b

        val c = bitScheduler.getAndShift(INT_BITS + 1)

        val c0 = Equality(
            c[0],
            Xor(
                a[0],
                b[0]
            )
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(INT_BITS)
        var pPrev = pArr[0]
        val p0 = Equality(
            pPrev,
            Conjunction(
                a[0],
                b[0]
            )
        )
        system.add(p0)

        for (i in 1 until INT_BITS) {
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

        return c
    }

    private fun parseIMUL(
        a: BitsArray,
        b: BitsArray,
        system: MutableList<BooleanFormula>,
        varSize: Int = INT_BITS
    ): BitsArray {

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

        return c
    }
}
