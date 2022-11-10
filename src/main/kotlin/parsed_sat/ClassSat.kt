package parsed_types

import bit_number_scheduler.BitNumberSchedulerImpl
import bits_field.BitsField
import extension.getBitsSize
import org.apache.bcel.classfile.JavaClass

class ClassSat(
    private val clazz: JavaClass,
    private val bitScheduler: BitNumberSchedulerImpl,
) {
    private val primaryTypesBitsMap = HashMap<String, BitsField>()

    // TODO: decide how to add the field below later
    // private val classTypesBitsMap

    private val _parsedMethods = HashMap<String, MethodSat>()
    val parsedMethods: Map<String, MethodSat> = _parsedMethods

    private var _name: String = clazz.className
    val name = _name

    init {
        for (field in clazz.fields) {
            val pos = bitScheduler.shift(field.type.getBitsSize())
        }
    }
}