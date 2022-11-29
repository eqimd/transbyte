package parsed_types

import bit_number_scheduler.BitsScheduler
import boolean_logic.BooleanFormula
import boolean_logic.additional.Equality
import boolean_logic.additional.Maj
import boolean_logic.additional.Xor
import boolean_logic.basis.Conjunction
import boolean_logic.basis.Disjunction
import boolean_logic.data.BitsArrayWithNumber
import constants.BooleanSystem
import constants.Constants.INT_BITS
import constants.Constants.LONG_BITS
import exception.MethodParseException
import exception.UnsupportedParseInstructionException
import extension.bitsSize
import extension.plus
import extension.times
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.BIPUSH
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

    fun parse(argsPrimitives: List<BitsArrayWithNumber>, argsArrays: List<ArraySat>, argsReferences: List<ClassSat>): MethodParseReturnValue {
        checkParseErrors(argsPrimitives, argsArrays, argsReferences)

        val localsPrimitives = HashMap<Int, BitsArrayWithNumber>()
        val localsArrays = HashMap<Int, ArraySat>()
        val localsReferences = HashMap<Int, ClassSat>()
        parseArgs(argsPrimitives, argsArrays, argsReferences, localsPrimitives, localsArrays, localsReferences)

        val stackPrimitives = ArrayDeque<BitsArrayWithNumber>()
        val stackReferences = ArrayDeque<ClassSat>()
        val stackArrays = ArrayDeque<ArraySat>()

        val system = emptyList<List<BooleanFormula>>().toMutableList()

        for (instrHandle in methodGen.instructionList) {
            when (val instruction = instrHandle.instruction) {
                is ILOAD -> {
                    stackPrimitives.addLast(localsPrimitives[instruction.index]!!)
                }
                is BIPUSH -> {
                    val value = instruction.value
                    val bitsArray = bitScheduler.getAndShift(INT_BITS)

                    stackPrimitives.addLast(
                        BitsArrayWithNumber(bitsArray, value)
                    )
                }
                is IADD, is LADD -> {
                    val a = stackPrimitives.removeLast()
                    val b = stackPrimitives.removeLast()
                    val varSize = if (instruction is IADD) INT_BITS else LONG_BITS

                    val c: BitsArrayWithNumber
                    if (a.constant != null && b.constant != null) {
                        val constC = a.constant + b.constant
                        c = BitsArrayWithNumber(bitScheduler.getAndShift(varSize), constC)
                    } else {
                        val (parsedVar, parseSystem) = parseIADD(a, b, varSize)
                        c = parsedVar
                        system.add(parseSystem)
                    }
                    stackPrimitives.addLast(c)
                }
                is IMUL, is LMUL -> {
                    val a = stackPrimitives.removeLast()
                    val b = stackPrimitives.removeLast()
                    val varSize = if (instruction is IMUL) INT_BITS else LONG_BITS
                    val c: BitsArrayWithNumber
                    if (a.constant != null && b.constant != null) {
                        val constC = a.constant * b.constant
                        c = BitsArrayWithNumber(bitScheduler.getAndShift(varSize), constC)
                    } else {
                        val (parsedVar, parseSystem) = parseIMUL(a, b, varSize)
                        c = parsedVar
                        system.add(parseSystem)
                    }
                    stackPrimitives.addLast(c)
                }
                is INVOKESTATIC -> {
                    // TODO should get parsing this instruction done

                    val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                    invokedMethod.parse(emptyList(), emptyList(), emptyList())
                }
                is IRETURN -> {
                    return MethodParseReturnValue(system, stackPrimitives.removeLast())
                }
                is RETURN -> {
                    return MethodParseReturnValue(system)
                }
                else -> {
                    throw UnsupportedParseInstructionException("Can't parse $instruction: instruction is not supported")
                }
            }
        }

        return MethodParseReturnValue(system)
    }

    private fun checkParseErrors(argsPrimitives: List<BitsArrayWithNumber>, argsArrays: List<ArraySat>, argsReferences: List<ClassSat>) {
        if (argsPrimitives.size != methodGen.argumentNames.size) {
            throw MethodParseException(
                "Number of arguments, given to .parse(...), is not same that is for" +
                    "`${clazz.className}` : `$method`"
            )
        }
        for ((index, pair) in (argsPrimitives zip methodGen.argumentTypes).withIndex()) {
            if (pair.first.bitsArray.size != pair.second.bitsSize) {
                throw MethodParseException(
                    "Size ${pair.first.bitsArray.size} of argument with index $index" +
                        "is not equal to ${pair.second.bitsSize}," +
                        "which is size of type ${pair.second}"
                )
            }
        }
    }

    private fun parseArgs(
        argsPrimitives: List<BitsArrayWithNumber>,
        argsArrays: List<ArraySat>,
        argsReferences: List<ClassSat>,
        localsPrimitives: HashMap<Int, BitsArrayWithNumber>,
        localsArrays: HashMap<Int, ArraySat>,
        localsReferences: HashMap<Int, ClassSat>
    ) {
        val argsPrimitivesIter = argsPrimitives.iterator()
        val argsArraysIter = argsArrays.iterator()
        val argsReferencesIter = argsReferences.iterator()

        for ((index, arg) in methodGen.argumentTypes.withIndex()) {
            when {
                arg.signature.startsWith('[') -> {
                    localsArrays[index] = argsArraysIter.next()
                }
                arg.signature.startsWith('L') -> {
                    localsReferences[index] = argsReferencesIter.next()
                }
                else -> {
                    localsPrimitives[index] = argsPrimitivesIter.next()
                }
            }
        }
    }

    private fun parseIADD(a: BitsArrayWithNumber, b: BitsArrayWithNumber, varSize: Int = INT_BITS): Pair<BitsArrayWithNumber, MutableList<BooleanFormula>> {
        // TODO should it consider overflowing?

        val system = emptyList<BooleanFormula>().toMutableList()

        // c = a + b
        val c = bitScheduler.getAndShift(varSize + 1)

        val c0 = Equality(
            c[0],
            Xor(
                a.bitsArray[0],
                b.bitsArray[0]
            )
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(varSize)
        var pPrev = pArr[0]
        val p0 = Equality(
            pPrev,
            Conjunction(
                a.bitsArray[0],
                b.bitsArray[0]
            )
        )
        system.add(p0)

        for (i in 1 until varSize) {
            val maj = Maj(
                a.bitsArray[i],
                b.bitsArray[i],
                pPrev
            )
            val xor = Xor(
                Xor(
                    a.bitsArray[i],
                    b.bitsArray[i]
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

        return Pair(
            BitsArrayWithNumber(c),
            system
        )
    }

    private fun parseIMUL(a: BitsArrayWithNumber, b: BitsArrayWithNumber, varSize: Int = INT_BITS): Pair<BitsArrayWithNumber, MutableList<BooleanFormula>> {
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
                        Conjunction(a.bitsArray[i], b.bitsArray[j])
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

        return Pair(
            BitsArrayWithNumber(c),
            system
        )
    }

    class MethodParseReturnValue(val system: BooleanSystem, val returnPrimitive: BitsArrayWithNumber? = null)
}
