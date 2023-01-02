package translator

import bit_scheduler.BitScheduler
import boolean_logic.additional.Equality
import extension.bitsSize
import mu.KotlinLogging
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BasicType
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.EncodingCircuit
import parsed_types.data.Variable
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
        vararg args: Variable
    ): EncodingCircuit {
        logger.info { "Translating method '$methodDescription' of class '$className'" }
        val classSat = classSatMap[className]!!
        val methodSat = classSat.getMethodByDescription(methodDescription)
            ?: throw RuntimeException("Class '$className' has no method '$methodDescription'")

        val circuitSystem = emptyList<Equality>().toMutableList()
        val methodSatArgs = if (args.isEmpty()) {
            Array(methodSat.methodGen.argumentTypes.size) { i ->
                when (val type = methodSat.methodGen.argumentTypes[i]) {
                    is ArrayType -> {
                        // TODO parse nested arrays
                        val (arg, _) = Variable.ArrayReference.ArrayOfPrimitives.create(
                            primitiveSize = type.basicType.bitsSize,
                            bitScheduler = bitScheduler
                        )

                        arg
                    }
                    is BasicType -> {
                        val (arg, _) = Variable.Primitive.create(
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
            else -> {
                TODO("Not supported yet")
            }
        }
    }
}
