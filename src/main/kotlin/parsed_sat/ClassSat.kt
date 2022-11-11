package parsed_sat

import bit_number_scheduler.BitsSchedulerImpl
import constants.BitsArray
import constants.ConstantPoolIndex
import extension.bitsSize
import extension.fullDescription
import org.apache.bcel.Const
import org.apache.bcel.classfile.ConstantFieldref
import org.apache.bcel.classfile.ConstantNameAndType
import org.apache.bcel.classfile.ConstantUtf8
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.ConstantPoolGen

class ClassSat(
    private val clazz: JavaClass,
    private val bitScheduler: BitsSchedulerImpl,
) {
    private val cpGen = ConstantPoolGen(clazz.constantPool)
    private val primaryTypesBitsMap = HashMap<ConstantPoolIndex, BitsArray>()

    // TODO decide how to add the field below later
    // private val classTypesBitsMap

    private val _parsedMethods = HashMap<String, MethodSat>()
    val parsedMethods: Map<String, MethodSat> = _parsedMethods

    val name = clazz.className

    init {
        for ((index, constant) in clazz.constantPool.withIndex()) {
            if ((constant?.tag ?: 0) == Const.CONSTANT_Fieldref) {
                println(index + 1)
                val constFieldref = constant as ConstantFieldref
                val nameAndType = clazz.constantPool.getConstant(constFieldref.nameAndTypeIndex, ConstantNameAndType::class.java)
                val utf = clazz.constantPool.getConstant(nameAndType.signatureIndex, ConstantUtf8::class.java)
                val type = BasicType.getType(utf.bytes)

                primaryTypesBitsMap[index + 1] = bitScheduler.getAndShift(type.bitsSize)
                println(type)
            }
        }

        for (method in clazz.methods) {
            _parsedMethods[method.fullDescription] = MethodSat(clazz, method, cpGen, bitScheduler)
        }
    }
}
