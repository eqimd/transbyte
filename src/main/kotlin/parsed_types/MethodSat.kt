package parsed_types

import bit_number_scheduler.BitNumberScheduler
import bits_field.BitsField
import boolean_formula.*
import constants.Constants.INT_BITS
import exception.MethodParseException
import exception.UnsupportedParseInstructionException
import extension.getBitsSize
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.*

class MethodSat(
    private val clazz: JavaClass,
    private val method: Method,
    private val bitScheduler: BitNumberScheduler,
) {
    private val methodGen = MethodGen(method, clazz.className, ConstantPoolGen(clazz.constantPool))

    fun parse(argsBitFields: List<BitsField>): List<BooleanFormula> {
        checkParseErrors(argsBitFields)

        val locals = parseArgsToLocals(argsBitFields)
        val stack = ArrayDeque<BitsField>()

        val system = emptyList<BooleanFormula>().toMutableList()

        for (instrHandle in methodGen.instructionList) {
            when (val instruction = instrHandle.instruction) {
                is ILOAD -> {
                    stack.addLast(locals[instruction.index])
                }
                is IADD -> {
                    // TODO should it consider overflowing?
                    val c = BitsField(bitScheduler.shift(INT_BITS + 1), INT_BITS + 1)
                    val a = stack.removeLast()
                    val b = stack.removeLast()

                    val c0 = Equality(
                        Bit(c.bitsPosition, false),
                        Xor(
                            Bit(a.bitsPosition, false),
                            Bit(b.bitsPosition, false)
                        )
                    )
                    system.add(c0)

                    val p = BitsField(bitScheduler.shift(INT_BITS), INT_BITS)
                    var pPrev = Bit(p.bitsPosition, false)
                    val p0 = Equality(
                        pPrev,
                        Conjunction(
                            Bit(a.bitsPosition, false),
                            Bit(b.bitsPosition, false)
                        )
                    )
                    system.add(p0)

                    for (i in 1 until INT_BITS) {
                        val maj = Maj(
                            Bit(a.bitsPosition + i, false),
                            Bit(b.bitsPosition + i, false),
                            pPrev
                        )
                        val xor = Xor(
                            Xor(
                                Bit(a.bitsPosition + i, false),
                                Bit(b.bitsPosition + i, false)
                            ),
                            pPrev
                        )
                        pPrev = Bit(p.bitsPosition + i, false)
                        val pI = Equality(
                            pPrev,
                            maj
                        )
                        val cI = Equality(
                            Bit(c.bitsPosition + i, false),
                            xor
                        )
                        system.add(pI)
                        system.add(cI)
                    }

                    val cLast = Equality(
                        Bit(c.bitsPosition + c.size - 1, false),
                        pPrev
                    )
                    system.add(cLast)

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

    private fun checkParseErrors(argsBitFields: List<BitsField>) {
        if (argsBitFields.size != methodGen.argumentNames.size) {
            throw MethodParseException(
                "Number of arguments, given to .parse(...), is not same that is for" +
                        "`${clazz.className}` : `$method`"
            )
        }
        for ((index, pair) in (argsBitFields zip methodGen.argumentTypes).withIndex()) {
            if (pair.first.size != pair.second.getBitsSize()) {
                throw MethodParseException(
                    "Size ${pair.first.size} of argument with index $index, bits position ${pair.first.bitsPosition}," +
                            "is not equal to ${pair.second.getBitsSize()}," +
                            "which is size of type ${pair.second}"
                )
            }
        }
    }

    private fun parseArgsToLocals(argsBitFields: List<BitsField>): Array<BitsField> {
        val locals = Array(6) { BitsField()}
        for ((index, elem) in argsBitFields.withIndex()) {
            locals[index] = elem
        }

        return locals
    }
}