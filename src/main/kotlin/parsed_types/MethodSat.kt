package parsed_types

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import constants.BooleanSystem
import constants.Constants.INT_BITS
import constants.Constants.LONG_BITS
import constants.MutableBooleanSystem
import exception.MethodParseException
import exception.ParseInstructionException
import instruction_parser.InstructionParser
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ASTORE
import org.apache.bcel.generic.BIPUSH
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.ICONST
import org.apache.bcel.generic.ILOAD
import org.apache.bcel.generic.IMUL
import org.apache.bcel.generic.INVOKESTATIC
import org.apache.bcel.generic.IRETURN
import org.apache.bcel.generic.ISTORE
import org.apache.bcel.generic.LADD
import org.apache.bcel.generic.LMUL
import org.apache.bcel.generic.MethodGen
import org.apache.bcel.generic.NEWARRAY
import org.apache.bcel.generic.RETURN
import parsed_types.data.Variable

class MethodSat(
    private val clazz: JavaClass,
    private val classSat: ClassSat,
    private val method: Method,
    cpGen: ConstantPoolGen,
    private val bitScheduler: BitScheduler,
) {
    private val methodGen = MethodGen(method, clazz.className, cpGen)

    val name: String = methodGen.name

    fun parse(vararg args: Variable): MethodParseReturnValue {
        val locals = HashMap<Int, Variable>()
        parseArgs(locals, *args)

        val stack = ArrayDeque<Variable>()

        val system: MutableBooleanSystem = emptyList<List<BooleanFormula>>().toMutableList()

        for (instrHandle in methodGen.instructionList) {
            when (val instruction = instrHandle.instruction) {
                is ASTORE -> {
                    when (val last = stack.removeLast()) {
                        is Variable.ArrayReference, is Variable.ClassReference -> {
                            locals[instruction.index] = last
                        }
                        else -> {
                            throw ParseInstructionException("Can't parse ASTORE: last stack variable is primitive")
                        }
                    }
                }
                is ILOAD -> {
                    stack.addLast(locals[instruction.index]!! as Variable.BitsArrayWithNumber)
                }
                is ICONST -> {
                    val (parsedBitsArray, parsedSystem) = InstructionParser.parsePUSH(instruction.value, bitScheduler)

                    stack.addLast(parsedBitsArray)
                    system.add(parsedSystem)
                }
                is ISTORE -> {
                    locals[instruction.index] = stack.removeLast() as Variable.BitsArrayWithNumber
                }
                is BIPUSH -> {
                    val (parsedBitsArray, parsedSystem) = InstructionParser.parsePUSH(instruction.value, bitScheduler)

                    stack.addLast(parsedBitsArray)
                    system.add(parsedSystem)
                }
                is NEWARRAY -> {
                    val size = stack.removeLast() as Variable.BitsArrayWithNumber
                    val arrayPrimitives = Variable.ArrayReference.ArrayPrimitives(
                        size.constant?.toInt(),
                        size.bitsArray.size
                    )

                    stack.addLast(arrayPrimitives)
                }
                is IADD, is LADD -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()

                    val varSize = if (instruction is IADD) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseADD(
                        a as Variable.BitsArrayWithNumber,
                        b as Variable.BitsArrayWithNumber,
                        bitScheduler,
                        varSize
                    )

                    stack.addLast(c)
                    system.add(parseSystem)
                }
                is IMUL, is LMUL -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()

                    val varSize = if (instruction is IMUL) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseMUL(
                        a as Variable.BitsArrayWithNumber,
                        b as Variable.BitsArrayWithNumber,
                        bitScheduler,
                        varSize
                    )

                    stack.addLast(c)
                    system.add(parseSystem)
                }
                is INVOKESTATIC -> {
                    // TODO should get parsing this instruction done

                    val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                    invokedMethod.parse()
                }
                is IRETURN -> {
                    return MethodParseReturnValue.SystemWithPrimitive(
                        system,
                        stack.removeLast() as Variable.BitsArrayWithNumber
                    )
                }
                is RETURN -> {
                    return MethodParseReturnValue.SystemOnly(system)
                }
                else -> {
                    throw ParseInstructionException("Can't parse $instruction: instruction is not supported")
                }
            }
        }

        return MethodParseReturnValue.SystemOnly(system)
    }

    private fun parseArgs(
        locals: HashMap<Int, Variable>,
        vararg args: Variable
    ) {
        if (args.size != methodGen.argumentTypes.size) {
            throw MethodParseException(
                "Can't parse call of method $name: given arguments count is ${args.size}, but is should be " +
                    methodGen.argumentTypes.size
            )
        }

        for ((index, arg) in methodGen.argumentTypes.withIndex()) {
            when {
                arg.signature.startsWith('[') -> {
                    locals[index] = args[index] as Variable.ArrayReference
                }
                arg.signature.startsWith('L') -> {
                    locals[index] = args[index] as Variable.ClassReference
                }
                else -> {
                    locals[index] = args[index] as Variable.BitsArrayWithNumber
                }
            }
        }
    }

    sealed interface MethodParseReturnValue {
        class SystemOnly(val system: BooleanSystem) : MethodParseReturnValue

        class SystemWithPrimitive(val system: BooleanSystem, val primitive: Variable.BitsArrayWithNumber) : MethodParseReturnValue

        class SystemWithArray(val system: BooleanSystem, val arrayReference: Variable.ArrayReference) : MethodParseReturnValue
    }
}
