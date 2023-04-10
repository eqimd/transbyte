package translator

import bit_scheduler.BitScheduler
import bit_scheduler.BitSchedulerImpl
import boolean_logic.BooleanVariable
import boolean_logic.Equality
import constants.Constants
import constants.GlobalSettings
import extension.bitsSize
import extension.fullDescription
import mu.KotlinLogging
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BasicType
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.EncodingCircuit
import parsed_types.data.VariableSat
import kotlin.RuntimeException

class BytecodeTranslatorImpl(classes: List<JavaClass>, arraySizes: List<Int> = emptyList()) : Translator {
    private val classSatMap = HashMap<String, ClassSat>()
    private val logger = KotlinLogging.logger {}

    private val bitScheduler: BitScheduler = BitSchedulerImpl(1)

    init {
        GlobalSettings.setupSettings(bitScheduler, arraySizes)

        for (clazz in classes) {
            classSatMap[clazz.className] = ClassSat(clazz)
        }
    }

    override fun translate(
        className: String,
        methodDescription: String?,
        vararg args: VariableSat
    ): EncodingCircuit {
        val classSat = classSatMap[className]
            ?: throw RuntimeException("Can't find class '$className'")

        val methodDescFinal = if (methodDescription == null) {
            // != 2 because every class has init method
            if (classSat.clazz.methods.size != 2) {
                throw RuntimeException(
                    "Class '$className' has more than 1 method, and the method for translation " +
                        "is not specifed"
                )
            }

            if (classSat.clazz.methods[0].name != Constants.INIT_METHOD_NAME) {
                classSat.clazz.methods[0].fullDescription
            } else {
                classSat.clazz.methods[1].fullDescription
            }
        } else {
            val methodsWithSameName = classSat.clazz.methods.filter { it.name == methodDescription }.toList()
            if (methodsWithSameName.size > 1) {
                throw RuntimeException(
                    "Class '$className' has more than one method with name 'methodDescription'. " +
                        "You need to specify full method signature, for example 'sum:(II)I'"
                )
            } else if (methodsWithSameName.size == 1) {
                methodsWithSameName.first().fullDescription
            } else {
                methodDescription
            }
        }

        logger.debug("Translating method '$methodDescFinal' of class '$className'")

        val methodSat = classSat.getMethodByDescription(methodDescFinal)
            ?: throw RuntimeException("Class '$className' has no method '$methodDescription'")

        val circuitSystem = emptyList<Equality>().toMutableList()

        var inputBits = emptyList<BooleanVariable.Bit>().toMutableList()

        val arraySizesIter = GlobalSettings.arraySizes.iterator()

        val methodSatArgs = if (args.isEmpty()) {
            Array(methodSat.methodGen.argumentTypes.size) { i ->
                when (val type = methodSat.methodGen.argumentTypes[i]) {
                    is ArrayType -> {
                        // TODO parse nested arrays
                        val arraySize: Int
                        try {
                            arraySize = arraySizesIter.next()
                        } catch (_: Exception) {
                            throw RuntimeException("Not enough array size parameters")
                        }

                        val prims = List(arraySize) { _ ->
                            VariableSat.Primitive.create(
                                size = type.basicType.bitsSize,
                            )
                        }.map { it.first }

                        val arg = VariableSat.ArrayReference(prims)

                        inputBits.addAll(
                            arg
                                .array
                                .map { it.bitsArray.toList() }
                                .flatten()
                        )

                        arg
                    }
                    is BasicType -> {
                        val (arg, _) = VariableSat.Primitive.create(
                            size = type.bitsSize
                        )

                        inputBits.addAll(
                            arg.bitsArray
                        )

                        arg
                    }
                    else -> {
                        logger.error("ReferenceType is not supported for translation yet")
                        TODO("ReferenceType is not supported for translation yet")
                    }
                }
            }
        } else args

        return when (val methodRetVal = methodSat.parse(*methodSatArgs)) {
            is MethodSat.MethodParseReturnValue.SystemOnly -> {
                circuitSystem.addAll(methodRetVal.system)

                EncodingCircuit(inputBits, null, circuitSystem)
            }
            is MethodSat.MethodParseReturnValue.SystemWithPrimitive -> {
                circuitSystem.addAll(methodRetVal.system)

                EncodingCircuit(inputBits, methodRetVal.primitive.bitsArray.toList(), circuitSystem)
            }
            is MethodSat.MethodParseReturnValue.SystemWithArray -> {
                // TODO now it works only with primitives

                circuitSystem.addAll(methodRetVal.system)
                val outputBits =
                    (methodRetVal.arrayReference as VariableSat.ArrayReference<VariableSat.Primitive>)
                        .array
                        .map { it.bitsArray.toList() }
                        .flatten()

                EncodingCircuit(inputBits, outputBits, circuitSystem)
            }
        }
    }
}
