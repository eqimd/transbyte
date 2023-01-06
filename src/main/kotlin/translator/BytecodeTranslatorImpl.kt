package translator

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import constants.Constants
import extension.bitsSize
import mu.KotlinLogging
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BasicType
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.EncodingCircuit
import parsed_types.data.VariableSat
import java.lang.RuntimeException

class BytecodeTranslatorImpl(vararg classes: JavaClass, private val bitScheduler: BitScheduler) : Translator {
    private val classSatMap = HashMap<String, ClassSat>()
    private val logger = KotlinLogging.logger {}
    init {
        for (clazz in classes) {
            classSatMap[clazz.className] = ClassSat(clazz, bitScheduler)
        }
    }

    override fun translate(
        className: String,
        methodDescription: String,
        vararg args: VariableSat
    ): EncodingCircuit {
        logger.info { "Translating method '$methodDescription' of class '$className'" }
        val classSat = classSatMap[className]!!
        val methodSat = classSat.getMethodByDescription(methodDescription)
            ?: throw RuntimeException("Class '$className' has no method '$methodDescription'")

        val circuitSystem = emptyList<BooleanFormula.Equality>().toMutableList()
        val methodSatArgs = if (args.isEmpty()) {
            Array(methodSat.methodGen.argumentTypes.size) { i ->
                when (val type = methodSat.methodGen.argumentTypes[i]) {
                    is ArrayType -> {
                        // TODO parse nested arrays
                        val (arg, _) = VariableSat.ArrayReference.ArrayOfPrimitives.create(
                            size = Constants.ARRAY_INPUT_SIZE,
                            primitiveSize = type.basicType.bitsSize,
                            bitScheduler = bitScheduler
                        )

                        arg
                    }
                    is BasicType -> {
                        val (arg, _) = VariableSat.Primitive.create(
                            bitScheduler = bitScheduler,
                            size = type.bitsSize
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
                EncodingCircuit(methodSatArgs.toList(), null, circuitSystem)
            }
            is MethodSat.MethodParseReturnValue.SystemWithPrimitive -> {
                circuitSystem.addAll(methodRetVal.system)
                EncodingCircuit(methodSatArgs.toList(), methodRetVal.primitive, circuitSystem)
            }
            is MethodSat.MethodParseReturnValue.SystemWithArray -> {
                circuitSystem.addAll(methodRetVal.system)
                EncodingCircuit(methodSatArgs.toList(), methodRetVal.arrayReference, circuitSystem)
            }
            else -> {
                TODO("Not supported yet")
            }
        }
    }
}
