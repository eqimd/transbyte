package class_sat_parser

import bit_number_scheduler.BitNumberScheduler
import bits_field.BitsField
import extension.getBitsSize
import org.apache.bcel.classfile.JavaClass

class ClassSatParser(
    private val clazz: JavaClass,
    private val bitScheduler: BitNumberScheduler,
) {
    private val fieldToBitsMap = HashMap<String, BitsField>()

    private val _parsedMethods = HashMap<String, ParsedMethod>()
    val parsedMethods: Map<String, ParsedMethod> = _parsedMethods

    init {
        for (field in clazz.fields) {
            val pos = bitScheduler.shiftPosition(field.type.getBitsSize())
        }
    }

    fun parse(): ParsedClass {

    }
}