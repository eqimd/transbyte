package parsed_types

import bit_scheduler.BitScheduler
import boolean_logic.BooleanVariable
import boolean_logic.Equality
import constants.BooleanSystem
import constants.GlobalSettings
import constants.MutableBooleanSystem
import exception.MethodParseException
import exception.ParseInstructionException
import extension.bitsSize
import mu.KotlinLogging
import operation_parser.OperationParser
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.ALOAD
import org.apache.bcel.generic.ARETURN
import org.apache.bcel.generic.ARRAYLENGTH
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
import org.apache.bcel.generic.IALOAD
import org.apache.bcel.generic.IAND
import org.apache.bcel.generic.IASTORE
import org.apache.bcel.generic.ICONST
import org.apache.bcel.generic.IFEQ
import org.apache.bcel.generic.IFLE
import org.apache.bcel.generic.IFLT
import org.apache.bcel.generic.IFNE
import org.apache.bcel.generic.IF_ICMPEQ
import org.apache.bcel.generic.IF_ICMPGE
import org.apache.bcel.generic.IF_ICMPGT
import org.apache.bcel.generic.IF_ICMPLE
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

        class SystemWithArray(val system: BooleanSystem, val arrayReference: VariableSat.ArrayReference<*>) :
            MethodParseReturnValue

        class SystemWithReference(val system: BooleanSystem, val reference: VariableSat.ClassReference)
    }

    internal data class ConditionCopy(
        val conditionBit: BooleanVariable.Bit,
        val locals: HashMap<Int, VariableSat>,
        val conditionInstructionPosition: Int,
        val jumpToInstructionPosition: Int,
        val inElseBranch: Boolean = false,
    )

    private inner class InnerInstructionParser {

        private val bitScheduler: BitScheduler = GlobalSettings.bitScheduler
        private val logger = KotlinLogging.logger {}

        private var locals = HashMap<Int, VariableSat>()
        private val stack = ArrayDeque<VariableSat>()
        private val system: MutableBooleanSystem = emptyList<Equality>().toMutableList()

        private val conditionStack = ArrayDeque<ConditionCopy>()

        private var instrIndex = 0

        fun parse(vararg args: VariableSat): MethodParseReturnValue {
            logger.debug("Parsing method '$name'")

            parseArgs(*args)

            while (instrIndex < methodGen.instructionList.instructions.size) {
                while (conditionStack.lastOrNull()?.jumpToInstructionPosition == instrIndex) {
                    logger.debug("Processing condition stack, size before: ${conditionStack.size}")
                    locals = parseConditionLocals(conditionStack.removeLast())
                    logger.debug("Processing condition stack, size after: ${conditionStack.size}")
                }

                val instruction = methodGen.instructionList.instructions[instrIndex]

                logger.debug("Bit scheduler positions: ${bitScheduler.currentPosition}")
                logger.debug("Parsing instruction '$instruction'")
                logger.debug("Instruction index: $instrIndex")

                when (instruction) {
                    is IF_ICMPGE -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseLessCondition(a, b)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = ${b.constant}")
                        parseConditionBit(condBit)
                    }

                    is IF_ICMPGT -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseLessOrEqualCondition(a, b)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = ${b.constant}")
                        parseConditionBit(condBit)
                    }

                    is IF_ICMPEQ -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // Condition is not reversed, but we should parse negated condition bit
                        val (condBit, condSystem) = OperationParser.parseEqualsCondition(a, b)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = ${b.constant}")

                        parseConditionBit(condBit.negated())
                    }

                    is IF_ICMPLE -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseLessCondition(b, a)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = ${b.constant}")
                        parseConditionBit(condBit)
                    }

                    is IF_ICMPNE -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseEqualsCondition(a, b)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = ${b.constant}")
                        parseConditionBit(condBit)
                    }

                    is IFLE -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseGreaterThanZero(a)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = 0")
                        parseConditionBit(condBit)
                    }

                    is IFLT -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        val (zero, sys) = VariableSat.Primitive.create(Int.SIZE_BITS, 0)
                        system.addAll(sys)

                        val (condBit, condSystem) = OperationParser.parseLessOrEqualCondition(zero, a)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = 0")
                        parseConditionBit(condBit)
                    }

                    is IFNE -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        // reversed condition, because if original condition is true then interpreter should jump forward
                        val (condBit, condSystem) = OperationParser.parseEqualToZero(a)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = 0")
                        parseConditionBit(condBit)
                    }

                    is IFEQ -> {
                        val a = stack.removeLast() as VariableSat.Primitive

                        val (condBit, condSystem) = OperationParser.parseEqualToZero(a)
                        system.addAll(condSystem)

                        logger.debug("Condition constants: a = ${a.constant} b = 0")
                        parseConditionBit(condBit.negated())
                    }

                    is GOTO -> {
                        val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
                        val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

                        if (instrJumpIndex < instrIndex) {
                            if (conditionStack.isNotEmpty()) {
                                var condLast = conditionStack.removeLast()
                                while (condLast.conditionInstructionPosition > instrJumpIndex) {
                                    logger.debug("Processing condition stack inside GOTO")
                                    locals = parseConditionLocals(condLast)
                                    condLast = conditionStack.removeLastOrNull() ?: break
                                }
                            }
                            instrIndex = instrJumpIndex - 1
                            // Cycle detected

//                        if (cycleIterationsStack.isNotEmpty()) {
//                            val last = cycleIterationsStack.removeLast()
//                            logger.debug("Cycle iteration: $last")
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
                                logger.debug("New condition in else-branch")
                                val condCopy = conditionStack.removeLast()

                                val newCond = ConditionCopy(
                                    conditionBit = condCopy.conditionBit,
                                    locals = locals,
                                    conditionInstructionPosition = instrIndex,
                                    jumpToInstructionPosition = instrJumpIndex,
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
                            is VariableSat.ArrayReference<*>, is VariableSat.ClassReference -> {
                                locals[instruction.index] = last
                            }

                            else -> {
                                throw ParseInstructionException("Can't parse ASTORE: last stack variable is primitive")
                            }
                        }
                    }

                    is BASTORE, is IASTORE -> {
                        val value = stack.removeLast() as VariableSat.Primitive
                        val index = stack.removeLast() as VariableSat.Primitive
                        val arrayRef = stack.removeLast() as VariableSat.ArrayReference<VariableSat.Primitive>

                        // TODO right now it works only when index constant is known or versions set is not empty
                        if (index.constant != null) {
                            arrayRef.array[index.constant.toInt()] = value
                        } else if (index.versions.isNotEmpty()) {
                            logger.debug("Parsing versions for array indexing")
                            for (v in index.versions) {
                                logger.debug("Version constant: ${v.key}")
                                val (newPrim, sys) = OperationParser.parseNewPrimitiveWithCondition(
                                    v.value,
                                    value,
                                    arrayRef.array[v.key.toInt()] as VariableSat.Primitive
                                )
                                system.addAll(sys)

                                arrayRef.array[v.key.toInt()] = newPrim
                            }
                        } else {
                            throw ParseInstructionException("Can't parse $instruction: index constant is null and there is no versions")
                        }
                    }

                    is ALOAD -> {
                        when (locals[instruction.index]) {
                            is VariableSat.ArrayReference<*>, is VariableSat.ClassReference -> {
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
                        val (parsedBitsArray, parsedSystem) = OperationParser.parsePush(instruction.value)

                        stack.addLast(parsedBitsArray)
                        system.addAll(parsedSystem)
                    }

                    is POP -> {
                        stack.removeLast()
                    }

                    is ISTORE -> {
                        locals[instruction.index] = stack.removeLast() as VariableSat.Primitive
                    }

                    is BALOAD, is IALOAD -> {
                        // TODO right now it works only when index is known or versions set is not empty
                        val index = stack.removeLast() as VariableSat.Primitive
                        val arrayRef = stack.removeLast() as VariableSat.ArrayReference<VariableSat.Primitive>
                        logger.debug("Index constant: ${index.constant}")

                        if (index.constant != null) {
                            stack.addLast(
                                arrayRef.array[index.constant.toInt()]
                            )
                        } else if (index.versions.isNotEmpty()) {
                            val primSize = arrayRef.array.first().bitsArray.size
                            val newPrim = VariableSat.Primitive.create(primSize).first

                            for (i in 0 until newPrim.bitsArray.size) {
                                logger.debug("Versions: ${index.versions.map { it.key }}")

                                val possibleBits = index.versions.map {
                                    val conjBit = bitScheduler.getAndShift(1).first()
                                    logger.debug("Possible bits constant: ${it.key}")
                                    system.add(
                                        Equality(
                                            conjBit,
                                            arrayRef.array[it.key.toInt()].bitsArray[i],
                                            it.value
                                        )
                                    )

                                    conjBit
                                }

                                val finalBit = possibleBits.reduce { b1, b2 ->
                                    val (disjBit, disjSys) = OperationParser.parseDisjunctionBits(b1, b2)
                                    system.addAll(disjSys)
                                    disjBit
                                }

                                system.add(
                                    Equality(
                                        newPrim.bitsArray[i],
                                        finalBit
                                    )
                                )
                            }

                            stack.addLast(newPrim)
                        } else {
                            throw ParseInstructionException("Can't parse $instruction: index constant is null and there is no versions")
                        }
                    }

                    is NEWARRAY -> {
                        val size = stack.removeLast() as VariableSat.Primitive
                        val arrayType = instruction.type as ArrayType

                        val list = List(size.constant!!.toInt()) { _ ->
                            VariableSat.Primitive.create(
                                size = arrayType.basicType.bitsSize,
                                constant = 0
                            )
                        }

                        val arrayPrims = VariableSat.ArrayReference(list.map { it.first })
                        val parseSystem = list.map { it.second }.flatten()

                        system.addAll(parseSystem)

                        stack.addLast(arrayPrims)
                    }

                    is ARRAYLENGTH -> {
                        // TODO now it works only with array of primitives
                        val arrayRef = stack.removeLast() as VariableSat.ArrayReference<*>
                        val (primSize, primSys) = VariableSat.Primitive.create(size = Int.SIZE_BITS, constant = arrayRef.array.size)
                        system.addAll(primSys)

                        stack.addLast(primSize)
                    }

                    is IADD, is LADD -> {
                        val a = stack.removeLast()
                        val b = stack.removeLast()

                        val (c, parseSystem) = OperationParser.parseSum(
                            a as VariableSat.Primitive,
                            b as VariableSat.Primitive
                        )

                        stack.addLast(c)
                        system.addAll(parseSystem)
                    }

                    is IMUL, is LMUL -> {
                        val a = stack.removeLast()
                        val b = stack.removeLast()

                        val (c, parseSystem) = OperationParser.parseMultiply(
                            a as VariableSat.Primitive,
                            b as VariableSat.Primitive
                        )

                        stack.addLast(c)
                        system.addAll(parseSystem)
                    }

                    is IINC -> {
                        val local = locals[instruction.index] as VariableSat.Primitive
                        val (incr, incrSys) = OperationParser.parsePush(instruction.increment)
                        system.addAll(incrSys)

                        val (c, parseSystem) = OperationParser.parseSum(
                            local,
                            incr
                        )

                        locals[instruction.index] = c
                        system.addAll(parseSystem)
                    }

                    is ISUB, is LSUB -> {
                        val b = stack.removeLast() as VariableSat.Primitive
                        val a = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = OperationParser.parseSubtraction(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IXOR, is LXOR -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = OperationParser.parseXor(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IOR, is LOR -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = OperationParser.parseOr(a, b)

                        system.addAll(parseSystem)
                        stack.addLast(c)
                    }

                    is IAND, is LAND -> {
                        val a = stack.removeLast() as VariableSat.Primitive
                        val b = stack.removeLast() as VariableSat.Primitive

                        val (c, parseSystem) = OperationParser.parseAnd(a, b)

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
                            val stackLast = stack.removeLast() as VariableSat.ArrayReference<*>
                            return MethodParseReturnValue.SystemWithArray(
                                system,
                                stackLast
                            )
                        } else {
                            logger.debug("Can't return references yet")
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

        private fun parseConditionBit(conditionBit: BooleanVariable) {
            val ih = methodGen.instructionList.instructionHandles[instrIndex] as BranchHandle
            val instrJumpIndex = methodGen.instructionList.instructionPositions.indexOf(ih.target.position)

            when (conditionBit) {
                is BooleanVariable.Constant -> {
                    if (conditionBit == BooleanVariable.Constant.FALSE) {
                        instrIndex = instrJumpIndex - 1
                    }
                }

                is BooleanVariable.Bit -> {
                    logger.debug("Encoding condition")
                    if (conditionStack.lastOrNull()?.jumpToInstructionPosition != instrIndex) {
                        conditionStack.addLast(
                            ConditionCopy(
                                conditionBit,
                                locals,
                                conditionInstructionPosition = instrIndex,
                                jumpToInstructionPosition = instrJumpIndex,
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
                        locals[index] = args[index] as VariableSat.ArrayReference<VariableSat.Primitive>
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
                if (locals[k] is VariableSat.ArrayReference<*>) {
                    val castedLocal = newLocals[k] as VariableSat.ArrayReference<*>
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
//                if (key == conditionCopy.cycleVariableIndex) {
//                    newLocals[key] = locals[key]!!
//                    continue
//                }

                when (val condLocal = conditionCopy.locals[key]) {
                    is VariableSat.Primitive -> {
                        val curLocal = locals[key] as VariableSat.Primitive
                        if (curLocal.bitsArray.first().bitNumber == condLocal.bitsArray.first().bitNumber) {
                            newLocals[key] = locals[key]!!
                            continue
                        }

                        val condTrueLocal: VariableSat.Primitive
                        val condFalseLocal: VariableSat.Primitive
                        if (conditionCopy.inElseBranch) {
                            condTrueLocal = condLocal
                            condFalseLocal = curLocal
                        } else {
                            condTrueLocal = curLocal
                            condFalseLocal = condLocal
                        }

                        logger.debug("Parsing new primitive with condition")
                        val (newLocal, condLocalsSystem) = OperationParser.parseNewPrimitiveWithCondition(
                            conditionCopy.conditionBit,
                            condTrueLocal,
                            condFalseLocal,
                        )

                        newLocals[key] = newLocal

                        system.addAll(condLocalsSystem)
                    }

                    // TODO now it works only with primitives
                    is VariableSat.ArrayReference<*> -> {
                        val cl = condLocal as VariableSat.ArrayReference<VariableSat.Primitive>
                        val curLocal = locals[key] as VariableSat.ArrayReference<VariableSat.Primitive>
                        val newLocal = condLocal.copy() as VariableSat.ArrayReference<VariableSat.Primitive>

                        newLocals[key] = newLocal

                        for (k in cl.array.indices) {
                            val condLocalPrim = condLocal.array[k]
                            val curLocalPrim = curLocal.array[k]

                            if (condLocalPrim.bitsArray.first() != curLocalPrim.bitsArray.first()) {
                                val condTrueLocal: VariableSat.Primitive
                                val condFalseLocal: VariableSat.Primitive
                                if (conditionCopy.inElseBranch) {
                                    condTrueLocal = condLocalPrim
                                    condFalseLocal = curLocalPrim
                                } else {
                                    condTrueLocal = curLocalPrim
                                    condFalseLocal = condLocalPrim
                                }

                                logger.debug("Parsing new primitive with condition (inside array of primitives)")
                                val (newPrim, condLocalsSystem) = OperationParser.parseNewPrimitiveWithCondition(
                                    conditionCopy.conditionBit,
                                    condTrueLocal,
                                    condFalseLocal,
                                )

                                newLocal.array[k] = newPrim

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
