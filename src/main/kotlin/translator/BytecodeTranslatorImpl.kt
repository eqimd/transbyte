package translator

import bit_scheduler.BitScheduler
import boolean_logic.additional.Equality
import extension.bitsSize
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.ReferenceType
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.EncodingCircuit
import parsed_types.data.Variable

class BytecodeTranslatorImpl(vararg classes: JavaClass, private val bitScheduler: BitScheduler) : Translator {
    private val classSatMap = HashMap<String, ClassSat>()
    init {
        for (clazz in classes) {
            classSatMap[clazz.className] = ClassSat(clazz, bitScheduler)
        }
    }

    override fun translate(className: String, methodDescription: String): EncodingCircuit {
        val classSat = classSatMap[className]!!
        val methodSat = classSat.getMethodByDescription(methodDescription)!!

        val args = emptyList<Variable>().toMutableList()
        val circuitSystem = emptyList<Equality>().toMutableList()

        for (type in methodSat.methodGen.argumentTypes) {
            when (type) {
                is ArrayType -> {
                    // TODO parse nested arrays
                    val (arg, _) = Variable.ArrayReference.ArrayOfPrimitives.create(
                        primitiveSize = type.basicType.bitsSize,
                        bitScheduler = bitScheduler
                    )

                    args.add(arg)
                }
                is ReferenceType -> {
                    TODO()
                }
                is BasicType -> {
                    val (arg, _) = Variable.Primitive.create(
                        bitScheduler = bitScheduler,
                        size = type.bitsSize
                    )

                    args.add(arg)
                }
            }
        }

        return when (val methodRetVal = methodSat.parse(*args.toTypedArray())) {
            is MethodSat.MethodParseReturnValue.SystemOnly -> {
                circuitSystem.addAll(methodRetVal.system)
                EncodingCircuit(args, null, circuitSystem)
            }
            is MethodSat.MethodParseReturnValue.SystemWithPrimitive -> {
                circuitSystem.addAll(methodRetVal.system)
                EncodingCircuit(args, methodRetVal.primitive, circuitSystem)
            }
            else -> {
                TODO("Not supported yet")
            }
        }
    }

    override fun translate(
        className: String,
        methodDescription: String,
        vararg args: Variable
    ): EncodingCircuit {
        val classSat = classSatMap[className]!!
        val methodSat = classSat.getMethodByDescription(methodDescription)!!

        return when (val methodRetVal = methodSat.parse(*args)) {
            is MethodSat.MethodParseReturnValue.SystemOnly -> {
                EncodingCircuit(args.toList(), null, methodRetVal.system)
            }
            is MethodSat.MethodParseReturnValue.SystemWithPrimitive -> {
                EncodingCircuit(args.toList(), methodRetVal.primitive, methodRetVal.system)
            }
            else -> {
                TODO("Not supported yet")
            }
        }
    }
}
