package parsed_types

import bit_number_scheduler.BitsSchedulerImpl
import constants.BitsArray
import constants.ConstantPoolIndex
import extension.bitsSize
import extension.fullDescription
import org.apache.bcel.Const
import org.apache.bcel.classfile.ConstantClass
import org.apache.bcel.classfile.ConstantFieldref
import org.apache.bcel.classfile.ConstantMethodref
import org.apache.bcel.classfile.ConstantNameAndType
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.ConstantPoolGen
import parsed_types.data.MethodRefNames

class ClassSat(
    private val clazz: JavaClass,
    private val bitScheduler: BitsSchedulerImpl,
) {
    private val cpGen = ConstantPoolGen(clazz.constantPool)
    private val primaryTypesBitsMap = HashMap<ConstantPoolIndex, BitsArray>()

    // TODO decide how to add the field below later
    // private val classTypesBitsMap

    private val methodRefs = HashMap<ConstantPoolIndex, MethodRefNames>()

    private val _methods = HashMap<String, MethodSat>()
    val methods: Map<String, MethodSat> = _methods

    val name: String = clazz.className

    init {
        // TODO parse constructor method in init section

        // Parse Fieldref and Methodref
        for ((index, constant) in clazz.constantPool.withIndex()) {
            when (constant?.tag) {
                Const.CONSTANT_Fieldref -> {
                    constant as ConstantFieldref
                    val nameAndType = clazz.constantPool.getConstant(constant.nameAndTypeIndex, ConstantNameAndType::class.java)
                    val utf = nameAndType.getSignature(clazz.constantPool)
                    val type = BasicType.getType(utf)

                    primaryTypesBitsMap[index] = bitScheduler.getAndShift(type.bitsSize)
                }
                Const.CONSTANT_Methodref -> {
                    constant as ConstantMethodref
                    val nameAndType = clazz.constantPool.getConstant(constant.nameAndTypeIndex, ConstantNameAndType::class.java)
                    val methodName = nameAndType.getName(clazz.constantPool)
                    val methodSignature = nameAndType.getSignature(clazz.constantPool)
                    val classConstant = clazz.constantPool.getConstant(constant.classIndex, ConstantClass::class.java)
                    val classUtf = clazz.constantPool.getConstantUtf8(classConstant.nameIndex)

                    methodRefs[index] = MethodRefNames(classUtf.bytes, "$methodName:$methodSignature")
                }
            }
        }

        for (method in clazz.methods) {
            _methods[method.fullDescription] = MethodSat(clazz, method, cpGen, bitScheduler)
        }
    }

    fun getMethodByDescription(description: String): MethodSat? =
        methods[description]

    fun getMethodByMethodrefIndex(index: Int): MethodSat? =
        methods[methodRefs[index]?.methodDescription]
}
