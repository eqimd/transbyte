package parsed_types

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import constants.BooleanSystem
import constants.GlobalSettings
import constants.MutableBooleanSystem
import exception.MethodParseException
import exception.ParseInstructionException
import extension.bitsSize
import instruction_parser.InstructionParser
import mu.KotlinLogging
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ALOAD
import org.apache.bcel.generic.ARETURN
import org.apache.bcel.generic.ASTORE
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BALOAD
import org.apache.bcel.generic.BASTORE
import org.apache.bcel.generic.BIPUSH
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.BranchHandle
import org.apache.bcel.generic.ConstantPoolGen
import org.apache.bcel.generic.ConstantPushInstruction
import org.apache.bcel.generic.GOTO
import org.apache.bcel.generic.IADD
import org.apache.bcel.generic.IAND
import org.apache.bcel.generic.ICONST
import org.apache.bcel.generic.IFLE
import org.apache.bcel.generic.IFNE
import org.apache.bcel.generic.IF_ICMPGE
import org.apache.bcel.generic.IF_ICMPNE
import org.apache.bcel.generic.IINC
import org.apache.bcel.generic.ILOAD
import org.apache.bcel.generic.IMUL
import org.apache.bcel.generic.INVOKESTATIC
import org.apache.bcel.generic.IOR
import org.apache.bcel.generic.IRETURN
import org.apache.bcel.generic.ISTORE
import org.apache.bcel.generic.ISUB
import org.apache.bcel.generic.IXOR
import org.apache.bcel.generic.LADD
import org.apache.bcel.generic.LAND
import org.apache.bcel.generic.LMUL
import org.apache.bcel.generic.LOR
import org.apache.bcel.generic.LSUB
import org.apache.bcel.generic.LXOR
import org.apache.bcel.generic.MethodGen
import org.apache.bcel.generic.NEWARRAY
import org.apache.bcel.generic.POP
import org.apache.bcel.generic.RETURN
import org.apache.bcel.generic.ReferenceType
import org.apache.bcel.generic.SIPUSH
import parsed_types.data.VariableSat
import java.lang.RuntimeException

class MethodSat(
    private val classSat: ClassSat,
    val method: Method,
    cpGen: ConstantPoolGen,
) {
    val methodGen = MethodGen(method, classSat.clazz.className, cpGen)

    val name: String = methodGen.name

    sealed interface MethodParseReturnValue {
        class SystemOnly(val system: BooleanSystem) : MethodParseReturnValue

        class SystemWithPrimitive(val system: BooleanSystem, val primitive: VariableSat.Primitive) :
            MethodParseReturnValue

        class SystemWithArray(val system: BooleanSystem, val arrayReference: VariableSat.ArrayReference) :
            MethodParseReturnValue

        class SystemWithReference(val system: BooleanSystem, val reference: VariableSat.ClassReference)
    }

    private data class ConditionCopy(
        val conditionBit: BooleanFormula.Variable.Bit,
        val locals: HashMap<Int, VariableSat>,
        val instructionPosition: Int,
        val inElseBranch: Boolean = false,
        var cycleVariableIndex: Int? = null
    )

    private inner class InnerInstructionParser {

        private val bitScheduler: BitScheduler = GlobalSettings.bitScheduler
        private val logger = KotlinLogging.logger {}

        private var locals = HashMap<Int, VariableSat>()
        private val stack = ArrayDeque<VariableSat>()
        private val system: MutableBooleanSystem = emptyList<BooleanFormula.Equality>().toMutableList()

        private val conditionStack = ArrayDeque<ConditionCopy>()

        private var instrIndex = 0

        fun parse(vararg args: VariableSat): MethodParseReturnValue {
            logger.debug { "Parsing method '$name'" }

            parseArgs(*args)

            while (instrIndex < methodGen.instructionList.instructions.size) {
                while (conditionStack.lastOrNull()?.instructionPosition == instrIndex) {
                    locals = parseConditionLocals(conditionStack.removeLast())
                }

                val instruction = methodGen.instructionList.instructions[instrIndex]

                logger.debug { "Bit scheduler positions: ${bitScheduler.currentPosition}" }
                logger.debug { "Parsing instruction '$instruction'" }
                logger.debug { "Instruction index: $instrIndex" }

                when (instruction) {
                    is IF_ICMPGE -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = InstructionParser.parseLessCondition(a, b)
                        system.addAll(condSystem)

                        parseConditionBit(condBit)
                    }

                    is IF_ICMPNE -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = InstructionParser.parseEqualsCondition(a, b)
                        system.addAll(condSystem)

                        parseConditionBit(condBit)
                    }

                    is IFLE -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = InstructionParser.parseGreaterThanZero(a)
                        system.addAll(condSystem)

                        parseConditionBit(condBit)
                    }

                    is IFNE -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = InstructionParser.parseEqualToZero(a)
                        system.addAll(condSystem)

                        parseConditionBit(condBit)
                    }

                    is GOTO -> {
                        val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
                        val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

                        if (instrJumpIndex < instrIndex) {
                            instrIndex = instrJumpIndex - 1
                            // Cycle detected

//                        if (cycleIterationsStack.isNotEmpty()) {
//                            val last = cycleIterationsStack.removeLast()
//                            logger.debug { "Cycle iteration: $last" }
//                            if (last > 0) {
//                                cycleIterationsStack.addLast(last - 1)
//                                instrIndex = instrJumpIndex - 1
//
//                                val instructionToJump =
//                                    methodGen.instructionList.instructions[instrIndex + 1] as LoadInstruction
//                                val localIndex = instructionToJump.index
//
//                                val condLast = conditionStack.removeLast()
//                                condLast.cycleVariableIndex = localIndex
//
//                                locals = parseConditionLocals(locals, condLast, system)
//                            } else {
//                                instrIndex = conditionStack.last().instructionPosition - 1
//                            }
//                        } else {
//                            cycleIterationsStack.addLast(CYCLE_ITERATIONS - 1)
//                            instrIndex = instrJumpIndex - 1
//
//                            val instructionToJump =
//                                methodGen.instructionList.instructions[instrIndex + 1] as LoadInstruction
//                            val localIndex = instructionToJump.index
//
//                            val condLast = conditionStack.removeLast()
//                            condLast.cycleVariableIndex = localIndex
//
//                            locals = parseConditionLocals(locals, condLast, system)
//                        }
                        } else {
                            // Next instructions will be from else-branch
                            if (conditionStack.isNotEmpty()) {
                                val condCopy = conditionStack.removeLast()

                                val newCond = ConditionCopy(
                                    conditionBit = condCopy.conditionBit,
                                    locals = locals,
                                    instructionPosition = instrJumpIndex,
                                    inElseBranch = true
                                )
                                conditionStack.addLast(newCond)

                                locals = condCopy.locals
                            } else {
                                instrIndex = instrJumpIndex - 1
                            }
                        }
                    }

                    is ASTORE -> {
                        when (val last = stack.removeLast()) {
                            is VariableSat.ArrayReference, is VariableSat.ClassReference -> {
                                locals[instruction.index] = last
                            }

                            else -> {
                                throw ParseInstructionException("Can't parse ASTORE: last stack variable is primitive")
                            }
                        }
                    }

                    is BASTORE -> {
                        val value = stack.removeLast() as VariableSat.Primitive
                        val index = stack.removeLast() as VariableSat.Primitive
                        val arrayRef = stack.removeLast() as VariableSat.ArrayReference.ArrayOfPrimitives

                        // TODO right now it works only when index constant is known
                        arrayRef.primitives[index.constant!!.toInt()] = value
                    }

                    is ALOAD -> {
                        when (locals[instruction.index]) {
                            is VariableSat.ArrayReference, is VariableSat.ClassReference -> {
                                stack.addLast(locals[instruction.index]!!)
                            }

                            else -> {
                                throw ParseInstructionException("Can't parse ALOAD: local variable is primitive")
                            }
                        }
                    }

                    is ILOAD -> {
                        stack.addLast(locals[instruction.index]!! as VariableSat.Primitive)
                    }

                    is ICONST, is BIPUSH, is SIPUSH -> {
                        instruction as ConstantPushInstruction
                        val (parsedBitsArray, parsedSystem) = InstructionParser.parsePush(instruction.value)

                        stack.addLast(parsedBitsArray)
                        system.addAll(parsedSystem)
                    }

                    is POP -> {
                        stack.removeLast()
                    }

                    is ISTORE -> {
                        locals[instruction.index] = stack.removeLast() as VariableSat.Primitive
                    }

                    is BALOAD -> {
                        // TODO right now it works only when index is known
                        val index = stack.removeLast() as VariableSat.Primitive
                        val arrayRef = stack.removeLast() as VariableSat.ArrayReference.ArrayOfPrimitives
                        logger.debug { "Index constant: ${index.constant}" }

                        stack.addLast(
                            arrayRef.primitives[index.constant] as VariableSat
                        )
                    }

                    is NEWARRAY -> {
                        val size = stack.removeLast() as VariableSat.Primitive
                        val arrayType = instruction.type as ArrayType
                        val (arrayPrimitives, parseSystem) = VariableSat.ArrayReference.ArrayOfPrimitives.create(
                            size = size.constant?.toInt(),
                            primitiveSize = arrayType.basicType.bitsSize,
                            constant = 0
                        )
                        system.addAll(parseSystem)

                        stack.addLast(arrayPrimitives)
                    }

                    is IADD, is LADD -> {
                        val a = stack.removeLast()
                        val b = stack.removeLast()

                        val (c, parseSystem) = InstructionParser.parseSum(
                            a as VariableSat.Primitive,
                            b as VariableSat.Primitive
                        )

                        stack.addLast(c)
                        system.addAll(parseSystem)
                    }

                    is IMUL, is LMUL -> {
                        val a = stack.removeLast()
                        val b = stack.removeLast()

                        val (c, parseSystem) = InstructionParser.parseMultiply(
                            a as VariableSat.Primitive,
                            b as VariableSat.Primitive
                        )

                        stack.addLast(c)
                        system.addAll(parseSystem)
                    }

                    is IINC -> {
                        val local = locals[instruction.index] as VariableSat.Primitive
                        val incr = InstructionParser.parsePush(instruction.increment).first

                        val (c, parseSystem) = InstructionParser.parseSum(
                            local,
                            incr
                        )

                        locals[instruction.index] = c
                        system.addAll(parseSystem)
                    }

                    is ISUB, is LSUB -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = InstructionParser.parseSubtraction(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IXOR, is LXOR -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = InstructionParser.parseXor(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IOR, is LOR -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = InstructionParser.parseOr(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IAND, is LAND -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = InstructionParser.parseAnd(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is INVOKESTATIC -> {
                        // TODO should get parsing this instruction done

                        val invokedMethod = classSat.getMethodByMethodrefIndex(instruction.index)!!
                        val argsCount = invokedMethod.methodGen.argumentTypes.size
                        val toArgs = Array(argsCount) { stack.removeLast() }.reversedArray()

                        when (val returnValue = invokedMethod.parse(*toArgs)) {
                            is MethodParseReturnValue.SystemOnly -> {
                                system.addAll(returnValue.system)
                            }

                            is MethodParseReturnValue.SystemWithPrimitive -> {
                                system.addAll(returnValue.system)

                                stack.addLast(returnValue.primitive)
                            }

                            is MethodParseReturnValue.SystemWithArray -> {
                                system.addAll(returnValue.system)

                                stack.addLast(returnValue.arrayReference)
                            }

                            else -> {
                                throw ParseInstructionException("Return value not supported yet")
                            }
                        }
                    }

                    is IRETURN -> {
                        return MethodParseReturnValue.SystemWithPrimitive(
                            system,
                            stack.removeLast() as VariableSat.Primitive
                        )
                    }

                    is ARETURN -> {
                        if (methodGen.returnType is ArrayType) {
                            val stackLast = stack.removeLast() as VariableSat.ArrayReference
                            return MethodParseReturnValue.SystemWithArray(
                                system,
                                stackLast
                            )
                        } else {
                            logger.debug { "Can't return references yet" }
                            TODO("Can't parse ARETURN: only returning arrays supported")
                        }
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

        private fun parseConditionBit(conditionBit: BooleanFormula.Variable) {
            val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
            val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

            when (conditionBit) {
                is BooleanFormula.Variable.Constant -> {
                    if (conditionBit == BooleanFormula.Variable.Constant.FALSE) {
                        instrIndex = instrJumpIndex - 1
                    }
                }

                is BooleanFormula.Variable.Bit -> {
                    if (conditionStack.lastOrNull()?.instructionPosition != instrIndex) {
                        conditionStack.addLast(
                            ConditionCopy(
                                conditionBit,
                                locals,
                                instrJumpIndex
                            )
                        )

                        locals = deepCopyLocals(locals)
                    }
                }
            }
        }

        private fun parseArgs(
            vararg args: VariableSat
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
                        locals[index] = args[index] as VariableSat.ArrayReference.ArrayOfPrimitives
                    }

                    is ReferenceType -> {
                        locals[index] = args[index] as VariableSat.ClassReference
                    }

                    is BasicType -> {
                        locals[index] = args[index] as VariableSat.Primitive
                    }
                }
            }
        }

        private fun deepCopyLocals(locals: Map<Int, VariableSat>): HashMap<Int, VariableSat> {
            val newLocals = HashMap(locals)

            for (k in locals.keys) {
                if (locals[k] is VariableSat.ArrayReference.ArrayOfPrimitives) {
                    val castedLocal = newLocals[k] as VariableSat.ArrayReference.ArrayOfPrimitives
                    val copyLocal = castedLocal.copy()
                    newLocals[k] = copyLocal
                }
            }

            return newLocals
        }

        private fun parseConditionLocals(
            conditionCopy: ConditionCopy,
        ): HashMap<Int, VariableSat> {
            val newLocals = HashMap<Int, VariableSat>()

            for (key in conditionCopy.locals.keys) {
                if (key == conditionCopy.cycleVariableIndex) {
                    newLocals[key] = locals[key]!!
                    continue
                }

                when (val condLocal = conditionCopy.locals[key]) {
                    is VariableSat.Primitive -> {
                        val curLocal = locals[key] as VariableSat.Primitive
                        if (curLocal.bitsArray.first().bitNumber == condLocal.bitsArray.first().bitNumber) {
                            newLocals[key] = locals[key]!!
                            continue
                        }

                        val newLocal = VariableSat.Primitive.create(
                            size = condLocal.bitsArray.size
                        ).first

                        newLocals[key] = newLocal

                        val condLocalsSystem = emptyList<BooleanFormula.Equality>().toMutableList()

                        for (i in 0 until condLocal.bitsArray.size) {
                            val condTrueBit: BooleanFormula.Variable.Bit
                            val condFalseBit: BooleanFormula.Variable.Bit
                            if (conditionCopy.inElseBranch) {
                                condTrueBit = condLocal.bitsArray[i]
                                condFalseBit = curLocal.bitsArray[i]
                            } else {
                                condTrueBit = curLocal.bitsArray[i]
                                condFalseBit = condLocal.bitsArray[i]
                            }

                            /* Condition:
                             *  x == (y and cond) or (z and not cond)
                             *  == not ( not(y and cond) and not(z and not cond))
                             *  == not (not A and not B)
                             *  == not C
                             *
                             *  Three new bits: A, B, C
                             */
                            val encBits = bitScheduler.getAndShift(3)

                            val lhsBit = encBits[0] // A
                            val lhsNegatedBit = lhsBit.negated() // not A
                            val rhsBit = encBits[1] // B
                            val rhsNegatedBit = rhsBit.negated() // not B
                            val fullBit = encBits[2] // C
                            val fullNegatedBit = fullBit.negated()

                            val negatedConditionCopyBit = conditionCopy.conditionBit.negated()

                            condLocalsSystem.addAll(
                                listOf(
                                    BooleanFormula.Equality(
                                        lhsBit,
                                        condTrueBit,
                                        conditionCopy.conditionBit
                                    ),
                                    BooleanFormula.Equality(
                                        rhsBit,
                                        condFalseBit,
                                        negatedConditionCopyBit
                                    ),
                                    BooleanFormula.Equality(
                                        fullBit,
                                        lhsNegatedBit,
                                        rhsNegatedBit
                                    ),
                                    BooleanFormula.Equality(
                                        newLocal.bitsArray[i],
                                        fullNegatedBit
                                    )
                                )
                            )
                        }

                        system.addAll(condLocalsSystem)
                    }

                    is VariableSat.ArrayReference.ArrayOfPrimitives -> {
                        val curLocal = locals[key] as VariableSat.ArrayReference.ArrayOfPrimitives
                        val newLocal = condLocal.copy()

                        newLocals[key] = newLocal

                        for (k in condLocal.primitives.keys) {
                            val condLocalPrim = condLocal.primitives[k]!!
                            val curLocalPrim = curLocal.primitives[k]!!

                            if (condLocalPrim.bitsArray.first() != curLocalPrim.bitsArray.first()) {
                                val newPrim = VariableSat.Primitive.create(
                                    size = curLocalPrim.bitsArray.size
                                ).first

                                newLocal.primitives[k] = newPrim

                                val condLocalsSystem = emptyList<BooleanFormula.Equality>().toMutableList()

                                for (i in 0 until condLocalPrim.bitsArray.size) {
                                    val condTrueBit: BooleanFormula.Variable.Bit
                                    val condFalseBit: BooleanFormula.Variable.Bit
                                    if (conditionCopy.inElseBranch) {
                                        condTrueBit = condLocalPrim.bitsArray[i]
                                        condFalseBit = curLocalPrim.bitsArray[i]
                                    } else {
                                        condTrueBit = curLocalPrim.bitsArray[i]
                                        condFalseBit = condLocalPrim.bitsArray[i]
                                    }

                                    // Same logic as above
                                    val encBits = bitScheduler.getAndShift(3)

                                    val lhsBit = encBits[0] // A
                                    val lhsNegatedBit = lhsBit.negated() // not A
                                    val rhsBit = encBits[1] // B
                                    val rhsNegatedBit = rhsBit.negated() // not B
                                    val fullBit = encBits[2] // C
                                    val fullNegatedBit = fullBit.negated()

                                    val negatedConditionCopyBit = conditionCopy.conditionBit.negated()

                                    condLocalsSystem.addAll(
                                        listOf(
                                            BooleanFormula.Equality(
                                                lhsBit,
                                                condTrueBit,
                                                conditionCopy.conditionBit
                                            ),
                                            BooleanFormula.Equality(
                                                rhsBit,
                                                condFalseBit,
                                                negatedConditionCopyBit
                                            ),
                                            BooleanFormula.Equality(
                                                fullBit,
                                                lhsNegatedBit,
                                                rhsNegatedBit
                                            ),
                                            BooleanFormula.Equality(
                                                newPrim.bitsArray[i],
                                                fullNegatedBit
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
    }

    fun parse(vararg args: VariableSat): MethodParseReturnValue {
        val innerInstructionParser = InnerInstructionParser()

        return innerInstructionParser.parse(*args)
    }
}
