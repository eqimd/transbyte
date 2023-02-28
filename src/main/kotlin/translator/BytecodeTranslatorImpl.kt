package translator

import bit_scheduler.BitScheduler
import bit_scheduler.BitSchedulerImpl
import boolean_logic.BooleanVariable
import boolean_logic.Equality
import constants.GlobalSettings
import extension.bitsSize
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
    private val primitiveConstants = HashMap<Number, VariableSat.Primitive>()
    init {
        GlobalSettings.setupSettings(bitScheduler, primitiveConstants, arraySizes)

        for (clazz in classes) {
            classSatMap[clazz.className] = ClassSat(clazz)
        }
    }

    override fun translate(
        className: String,
        methodDescription: String,
        vararg args: VariableSat
    ): EncodingCircuit {
        logger.debug("Translating method '$methodDescription' of class '$className'")
        val classSat = classSatMap[className]!!
        val methodSat = classSat.getMethodByDescription(methodDescription)
            ?: throw RuntimeException("Class '$className' has no method '$methodDescription'")

        val circuitSystem = emptyList<Equality>().toMutableList()

        var inputBits = emptyList<BooleanVariable.Bit>().toMutableList()

        val arraySizesIter = GlobalSettings.arraySizes.iterator()

        val methodSatArgs = if (args.isEmpty()) {
            Array(methodSat.methodGen.argumentTypes.size) { i ->
                when (val type = methodSat.methodGen.argumentTypes[i]) {
                    is ArrayType -> {
                        // TODO parse nested arrays
                        val (arg, _) = VariableSat.ArrayReference.ArrayOfPrimitives.create(
                            size = arraySizesIter.next(),
                            primitiveSize = type.basicType.bitsSize,
                        )

                        inputBits.addAll(
                            (arg as VariableSat.ArrayReference.ArrayOfPrimitives)
                                .primitives
                                .values
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
                        logger.error { "ReferenceType is not supported for translation yet" }
                        TODO()
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
                circuitSystem.addAll(methodRetVal.system)
                val outputBits =
                    (methodRetVal.arrayReference as VariableSat.ArrayReference.ArrayOfPrimitives)
                        .primitives
                        .values
                        .map { it.bitsArray.toList() }
                        .flatten()

                EncodingCircuit(inputBits, outputBits, circuitSystem)
            }
            else -> {
                TODO("Not supported yet")
            }
        }
    }
}
