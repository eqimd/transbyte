package parsed_types

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import boolean_logic.additional.Equality
import boolean_logic.base.Conjunction
import boolean_logic.base.Disjunction
import boolean_logic.base.Negated
import constants.BooleanSystem
import constants.Constants.CYCLE_ITERATIONS
import constants.Constants.INT_BITS
import constants.Constants.LONG_BITS
import constants.MutableBooleanSystem
import exception.MethodParseException
import exception.ParseInstructionException
import extension.bitsSize
import instruction_parser.InstructionParser
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ALOAD
import org.apache.bcel.generic.ASTORE
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BASTORE
import org.apache.bcel.generic.BIPUSH
import org.apache.bcel.generic.BranchHandle
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.GOTO
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.ICONST
import org.apache.bcel.generic.IF_ICMPGE
import org.apache.bcel.generic.IINC
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
import java.lang.RuntimeException
import kotlin.math.abs

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
        var locals = HashMap<Int, Variable>()
        parseArgs(locals, *args)

        val stack = ArrayDeque<Variable>()
        val system: MutableBooleanSystem = emptyList<List<BooleanFormula>>().toMutableList()

        val conditionStack = ArrayDeque<ConditionCopy>()
        val cycleIterationsStack = ArrayDeque<Int>()

        var instrIndex = 0
        while (instrIndex < methodGen.instructionList.instructions.size) {
            if (conditionStack.lastOrNull()?.instructionPosition == instrIndex) {
                locals = parseConditionLocals(locals, conditionStack.removeLast(), system)
            }

            when (val instruction = methodGen.instructionList.instructions[instrIndex]) {
                is IF_ICMPGE -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()

                    val condition = InstructionParser.parseLessCondition(
                        a as Variable.BitsArrayWithNumber,
                        b as Variable.BitsArrayWithNumber
                    )

                    // TODO do we need this optimisation?
//                    if (condition == BitValue.FALSE) {
//                        instrIndex = methodGen.instructionList.instructionPositions.indexOf(instruction.index) - 1
//                    }

                    // TODO constants is not necessary ints
                    if (a.constant != null && b.constant != null) {
                        cycleIterationsStack.addLast(abs(a.constant.toInt() - b.constant.toInt()))
                    }

                    val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
                    val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

                    if (conditionStack.lastOrNull()?.instructionPosition != instrIndex) {
                        conditionStack.addLast(
                            ConditionCopy(
                                condition,
                                locals,
                                instrJumpIndex
                            )
                        )

                        locals = HashMap(locals)
                    }
                }
                is GOTO -> {
                    val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
                    val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

                    if (instrJumpIndex < instrIndex) {
                        // Cycle detected
                        if (cycleIterationsStack.isNotEmpty()) {
                            val last = cycleIterationsStack.removeLast()
                            if (last != 0) {
                                cycleIterationsStack.addLast(last - 1)
                                instrIndex = instrJumpIndex - 1

                                locals = parseConditionLocals(locals, conditionStack.removeLast(), system)
                            } else {
                                instrIndex = conditionStack.last().instructionPosition - 1
                            }
                        } else {
                            cycleIterationsStack.addLast(CYCLE_ITERATIONS - 1)
                            instrIndex = instrJumpIndex - 1

                            locals = parseConditionLocals(locals, conditionStack.removeLast(), system)
                        }
                    } else {
                        // Next instructions will be from else-branch
                        val condCopy = conditionStack.removeLast()
                        val newCond = ConditionCopy(
                            Negated(condCopy.condition),
                            locals,
                            instrJumpIndex
                        )
                        conditionStack.addLast(newCond)

                        locals = condCopy.locals
                    }
                }
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
                is BASTORE -> {
                    val value = stack.removeLast() as Variable.BitsArrayWithNumber
                    val index = stack.removeLast() as Variable.BitsArrayWithNumber
                    val arrayRef = stack.removeLast() as Variable.ArrayReference.ArrayPrimitives

                    // TODO right now it works only when index constant is known
                    arrayRef.primitives[index.constant!!.toInt()] = value
                }
                is ALOAD -> {
                    when (locals[instruction.index]) {
                        is Variable.ArrayReference, is Variable.ClassReference -> {
                            stack.addLast(locals[instruction.index]!!)
                        }
                        else -> {
                            throw ParseInstructionException("Can't parse ALOAD: local variable is primitive")
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
                    val arrayType = instruction.type as ArrayType
                    val arrayPrimitives = Variable.ArrayReference.ArrayPrimitives(
                        size.constant?.toInt(),
                        arrayType.basicType.bitsSize,
                        bitScheduler
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
                is IINC -> {
                    val local = locals[instruction.index] as Variable.BitsArrayWithNumber
                    val incr = Variable.BitsArrayWithNumber(
                        bitScheduler.getAndShift(INT_BITS),
                        instruction.increment
                    )

                    val (c, parseSystem) = InstructionParser.parseADD(
                        local,
                        incr,
                        bitScheduler,
                        INT_BITS
                    )

                    locals[instruction.index] = c
                    system.add(parseSystem)
                }
                is INVOKESTATIC -> {
                    // TODO should get parsing this instruction done

                    val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                    val argsCount = invokedMethod.methodGen.argumentTypes.size
                    val toArgs = Array(argsCount) { stack.removeLast() }

                    when (val returnValue = invokedMethod.parse(*toArgs)) {
                        is MethodParseReturnValue.SystemOnly -> {
                            if (returnValue.system.isNotEmpty()) {
                                system.add(returnValue.system.flatten())
                            }
                        }
                        is MethodParseReturnValue.SystemWithPrimitive -> {
                            if (returnValue.system.isNotEmpty()) {
                                system.add(returnValue.system.flatten())
                            }

                            stack.addLast(returnValue.primitive)
                        }
                        else -> {
                            throw ParseInstructionException("Return value not supported yet")
                        }
                    }
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

            instrIndex++
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

    private fun parseConditionLocals(
        locals: HashMap<Int, Variable>,
        conditionCopy: ConditionCopy,
        system: MutableList<List<BooleanFormula>>
    ): HashMap<Int, Variable> {
        val newLocals = HashMap<Int, Variable>()

        for (key in conditionCopy.locals.keys) {
            when (val condLocal = conditionCopy.locals[key]) {
                is Variable.BitsArrayWithNumber -> {
                    val curLocal = locals[key] as Variable.BitsArrayWithNumber
                    if (curLocal.bitsArray.first().bitNumber == condLocal.bitsArray.first().bitNumber) {
                        newLocals[key] = locals[key]!!
                        continue
                    }

                    val newLocal = bitScheduler.getAndShift(condLocal.bitsArray.size)
                    newLocals[key] = Variable.BitsArrayWithNumber(newLocal)
                    val condLocalsSystem = emptyList<BooleanFormula>().toMutableList()
                    for (i in 0 until condLocal.bitsArray.size) {
                        condLocalsSystem.add(
                            Equality(
                                newLocal[i],
                                Disjunction(
                                    Conjunction(
                                        curLocal.bitsArray[i],
                                        conditionCopy.condition
                                    ),
                                    Conjunction(
                                        condLocal.bitsArray[i],
                                        Negated(conditionCopy.condition)
                                    )
                                )
                            )
                        )
                    }

                    system.add(condLocalsSystem)
                }
                else -> {
                    throw RuntimeException("Only primitive variables supported right now")
                }
            }
        }

        return newLocals
    }

    sealed interface MethodParseReturnValue {
        class SystemOnly(val system: BooleanSystem) : MethodParseReturnValue

        class SystemWithPrimitive(val system: BooleanSystem, val primitive: Variable.BitsArrayWithNumber) : MethodParseReturnValue

        class SystemWithArray(val system: BooleanSystem, val arrayReference: Variable.ArrayReference) : MethodParseReturnValue
    }

    data class ConditionCopy(
        val condition: BooleanFormula,
        val locals: HashMap<Int, Variable>,
        val instructionPosition: Int,
    )
}
