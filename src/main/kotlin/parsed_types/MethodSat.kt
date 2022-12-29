package parsed_types

import bit_scheduler.BitScheduler
import boolean_logic.additional.Equality
import boolean_logic.base.BitVariable
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
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ALOAD
import org.apache.bcel.generic.ASTORE
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BALOAD
import org.apache.bcel.generic.BASTORE
import org.apache.bcel.generic.BIPUSH
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.BranchHandle
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.GOTO
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.ICONST
import org.apache.bcel.generic.IFLE
import org.apache.bcel.generic.IF_ICMPGE
import org.apache.bcel.generic.IINC
import org.apache.bcel.generic.ILOAD
import org.apache.bcel.generic.IMUL
import org.apache.bcel.generic.INVOKESTATIC
import org.apache.bcel.generic.IRETURN
import org.apache.bcel.generic.ISTORE
import org.apache.bcel.generic.ISUB
import org.apache.bcel.generic.IXOR
import org.apache.bcel.generic.LADD
import org.apache.bcel.generic.LMUL
import org.apache.bcel.generic.LSUB
import org.apache.bcel.generic.LXOR
import org.apache.bcel.generic.MethodGen
import org.apache.bcel.generic.NEWARRAY
import org.apache.bcel.generic.RETURN
import org.apache.bcel.generic.ReferenceType
import parsed_types.data.Variable
import java.lang.RuntimeException
import kotlin.math.abs

class MethodSat(
    private val classSat: ClassSat,
    val method: Method,
    cpGen: ConstantPoolGen,
    private val bitScheduler: BitScheduler,
) {
    val methodGen = MethodGen(method, classSat.clazz.className, cpGen)

    val name: String = methodGen.name

    fun parse(vararg args: Variable): MethodParseReturnValue {
        var locals = HashMap<Int, Variable>()
        parseArgs(locals, *args)

        val stack = ArrayDeque<Variable>()
        val system: MutableBooleanSystem = emptyList<Equality>().toMutableList()

        val conditionStack = ArrayDeque<ConditionCopy>()
        val cycleIterationsStack = ArrayDeque<Int>()

        var instrIndex = 0
        while (instrIndex < methodGen.instructionList.instructions.size) {
            while (conditionStack.lastOrNull()?.instructionPosition == instrIndex) {
                locals = parseConditionLocals(locals, conditionStack.removeLast(), system)
            }

            val instruction = methodGen.instructionList.instructions[instrIndex]
            when (instruction) {
                is IF_ICMPGE -> {
                    val b = stack.removeLast() as Variable.BitsArrayWithNumber
                    val a = stack.removeLast() as Variable.BitsArrayWithNumber

                    val (condBit, condSystem) = InstructionParser.parseLessCondition(a, b, bitScheduler)
                    system.addAll(condSystem)

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
                                condBit,
                                locals,
                                instrJumpIndex
                            )
                        )

                        // TODO deep copy of ArrayOfPrimitive
                        locals = deepCopyLocals(locals)
                    }
                }
                is IFLE -> {
                    val a = stack.removeLast() as Variable.BitsArrayWithNumber

                    // reversed condition, because if original condition is true then interpreter should jump forward
                    val (condBit, condSystem) = InstructionParser.parseGreaterThanZero(a, bitScheduler)

                    system.addAll(condSystem)

                    // TODO do we need this optimisation?
//                    if (condition == BitValue.FALSE) {
//                        instrIndex = methodGen.instructionList.instructionPositions.indexOf(instruction.index) - 1
//                    }

                    // TODO constant is not necessary int
                    if (a.constant != null) {
                        cycleIterationsStack.addLast(a.constant.toInt())
                    }

                    val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
                    val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

                    if (conditionStack.lastOrNull()?.instructionPosition != instrIndex) {
                        conditionStack.addLast(
                            ConditionCopy(
                                condBit,
                                locals,
                                instrJumpIndex
                            )
                        )

                        // TODO deep copy of ArrayOfPrimitives
                        locals = deepCopyLocals(locals)
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
                            conditionBit = condCopy.conditionBit,
                            locals = locals,
                            instructionPosition = instrJumpIndex,
                            inElseBranch = true
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
                    val arrayRef = stack.removeLast() as Variable.ArrayReference.ArrayOfPrimitives

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
                    val (parsedBitsArray, parsedSystem) = InstructionParser.parsePush(instruction.value, bitScheduler)

                    stack.addLast(parsedBitsArray)
                    system.addAll(parsedSystem)
                }

                is ISTORE -> {
                    locals[instruction.index] = stack.removeLast() as Variable.BitsArrayWithNumber
                }

                is BIPUSH -> {
                    val (parsedBitsArray, parsedSystem) = InstructionParser.parsePush(instruction.value, bitScheduler)

                    stack.addLast(parsedBitsArray)
                    system.addAll(parsedSystem)
                }

                is BALOAD -> {
                    // TODO right now it works only when index is known
                    val index = stack.removeLast() as Variable.BitsArrayWithNumber
                    val arrayRef = stack.removeLast() as Variable.ArrayReference.ArrayOfPrimitives

                    stack.addLast(
                        arrayRef.primitives[index.constant] as Variable
                    )
                }

                is NEWARRAY -> {
                    val size = stack.removeLast() as Variable.BitsArrayWithNumber
                    val arrayType = instruction.type as ArrayType
                    val (arrayPrimitives, parseSystem) = Variable.ArrayReference.ArrayOfPrimitives.create(
                        size = size.constant?.toInt(),
                        primitiveSize = arrayType.basicType.bitsSize,
                        bitScheduler = bitScheduler
                    )
                    system.addAll(parseSystem)

                    stack.addLast(arrayPrimitives)
                }

                is IADD, is LADD -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()

                    val varSize = if (instruction is IADD) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseSum(
                        a as Variable.BitsArrayWithNumber,
                        b as Variable.BitsArrayWithNumber,
                        varSize,
                        bitScheduler
                    )

                    stack.addLast(c)
                    system.addAll(parseSystem)
                }

                is IMUL, is LMUL -> {
                    val a = stack.removeLast()
                    val b = stack.removeLast()

                    val varSize = if (instruction is IMUL) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseMultiply(
                        a as Variable.BitsArrayWithNumber,
                        b as Variable.BitsArrayWithNumber,
                        varSize,
                        bitScheduler
                    )

                    stack.addLast(c)
                    system.addAll(parseSystem)
                }

                is IINC -> {
                    val local = locals[instruction.index] as Variable.BitsArrayWithNumber
                    val (incr, _) = Variable.BitsArrayWithNumber.create(
                        size = INT_BITS,
                        constant = instruction.increment,
                        bitScheduler = bitScheduler
                    )

                    val (c, parseSystem) = InstructionParser.parseSum(
                        local,
                        incr,
                        INT_BITS,
                        bitScheduler
                    )

                    locals[instruction.index] = c
                    system.addAll(parseSystem)
                }

                is ISUB, is LSUB -> {
                    val b = stack.removeLast() as Variable.BitsArrayWithNumber
                    val a = stack.removeLast() as Variable.BitsArrayWithNumber

                    val varSize = if (instruction is ISUB) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseSubtraction(a, b, varSize, bitScheduler)

                    system.addAll(parseSystem)
                    stack.addLast(c)
                }

                is IXOR, is LXOR -> {
                    val a = stack.removeLast() as Variable.BitsArrayWithNumber
                    val b = stack.removeLast() as Variable.BitsArrayWithNumber

                    val varSize = if (instruction is IXOR) INT_BITS else LONG_BITS
                    val (c, parseSystem) = InstructionParser.parseXor(a, b, varSize, bitScheduler)

                    system.addAll(parseSystem)
                    stack.addLast(c)
                }

                is INVOKESTATIC -> {
                    // TODO should get parsing this instruction done

                    val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                    val argsCount = invokedMethod.methodGen.argumentTypes.size
                    val toArgs = Array(argsCount) { stack.removeLast() }

                    when (val returnValue = invokedMethod.parse(*toArgs)) {
                        is MethodParseReturnValue.SystemOnly -> {
                            if (returnValue.system.isNotEmpty()) {
                                system.addAll(returnValue.system)
                            }
                        }

                        is MethodParseReturnValue.SystemWithPrimitive -> {
                            if (returnValue.system.isNotEmpty()) {
                                system.addAll(returnValue.system)
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
                "Can't parse call of method $name: given arguments count is ${args.size}, but it should be " +
                    methodGen.argumentTypes.size
            )
        }

        for ((index, arg) in methodGen.argumentTypes.withIndex()) {
            when (arg) {
                is ArrayType -> {
                    locals[index] = args[index] as Variable.ArrayReference
                }

                is ReferenceType -> {
                    locals[index] = args[index] as Variable.ClassReference
                }

                is BasicType -> {
                    locals[index] = args[index] as Variable.BitsArrayWithNumber
                }
            }
        }
    }

    private fun parseConditionLocals(
        locals: Map<Int, Variable>,
        conditionCopy: ConditionCopy,
        system: MutableList<Equality>
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

                    val newLocal = Variable.BitsArrayWithNumber.create(
                        size = condLocal.bitsArray.size,
                        bitScheduler = bitScheduler
                    ).first

                    newLocals[key] = newLocal

                    val condLocalsSystem = emptyList<Equality>().toMutableList()

                    for (i in 0 until condLocal.bitsArray.size) {
                        val condTrueBit: BitVariable
                        val condFalseBit: BitVariable
                        if (conditionCopy.inElseBranch) {
                            condTrueBit = condLocal.bitsArray[i]
                            condFalseBit = curLocal.bitsArray[i]
                        } else {
                            condTrueBit = curLocal.bitsArray[i]
                            condFalseBit = condLocal.bitsArray[i]
                        }

                        condLocalsSystem.add(
                            Equality(
                                newLocal.bitsArray[i],
                                Disjunction(
                                    Conjunction(
                                        condTrueBit,
                                        conditionCopy.conditionBit
                                    ),
                                    Conjunction(
                                        condFalseBit,
                                        Negated(conditionCopy.conditionBit)
                                    )
                                )
                            )
                        )
                    }

                    system.addAll(condLocalsSystem)
                }
                is Variable.ArrayReference.ArrayOfPrimitives -> {
                    val curLocal = locals[key] as Variable.ArrayReference.ArrayOfPrimitives
                    val newLocal = condLocal.copy()

                    newLocals[key] = newLocal

                    for (k in condLocal.primitives.keys) {
                        val condLocalPrim = condLocal.primitives[k]!!
                        val curLocalPrim = curLocal.primitives[k]!!

                        if (condLocalPrim.bitsArray.first() != curLocalPrim.bitsArray.first()) {
                            val newPrim = Variable.BitsArrayWithNumber.create(
                                size = curLocalPrim.bitsArray.size,
                                bitScheduler = bitScheduler
                            ).first

                            newLocal.primitives[k] = newPrim

                            val condLocalsSystem = emptyList<Equality>().toMutableList()

                            for (i in 0 until condLocalPrim.bitsArray.size) {
                                val condTrueBit: BitVariable
                                val condFalseBit: BitVariable
                                if (conditionCopy.inElseBranch) {
                                    condTrueBit = condLocalPrim.bitsArray[i]
                                    condFalseBit = curLocalPrim.bitsArray[i]
                                } else {
                                    condTrueBit = curLocalPrim.bitsArray[i]
                                    condFalseBit = condLocalPrim.bitsArray[i]
                                }

                                condLocalsSystem.add(
                                    Equality(
                                        newPrim.bitsArray[i],
                                        Disjunction(
                                            Conjunction(
                                                condTrueBit,
                                                conditionCopy.conditionBit
                                            ),
                                            Conjunction(
                                                condFalseBit,
                                                Negated(conditionCopy.conditionBit)
                                            )
                                        )
                                    )
                                )
                            }

                            system.addAll(condLocalsSystem)
                        }
                    }
                }
                else -> {
                    throw RuntimeException("Not supported right now")
                }
            }
        }

        return newLocals
    }

    private fun deepCopyLocals(locals: HashMap<Int, Variable>): HashMap<Int, Variable> {
        val newLocals = HashMap(locals)

        for (k in locals.keys) {
            if (locals[k] is Variable.ArrayReference.ArrayOfPrimitives) {
                val castedLocal = newLocals[k] as Variable.ArrayReference.ArrayOfPrimitives
                val copyLocal = castedLocal.copy()
                newLocals[k] = copyLocal
            }
        }

        return newLocals
    }

    sealed interface MethodParseReturnValue {
        class SystemOnly(val system: BooleanSystem) : MethodParseReturnValue

        class SystemWithPrimitive(val system: BooleanSystem, val primitive: Variable.BitsArrayWithNumber) :
            MethodParseReturnValue

        class SystemWithArray(val system: BooleanSystem, val arrayReference: Variable.ArrayReference) :
            MethodParseReturnValue

        class SystemWithReference(val system: BooleanSystem, val reference: Variable.ClassReference)
    }

    data class ConditionCopy(
        val conditionBit: BitVariable,
        val locals: HashMap<Int, Variable>,
        val instructionPosition: Int,
        val inElseBranch: Boolean = false
    )
}
