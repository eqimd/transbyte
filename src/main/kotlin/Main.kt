import bit_number_scheduler.BitsSchedulerImpl
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_sat.ClassSat

fun main(args: Array<String>) {
    val classFilePath = "docs/examples/Sum/S.class"
    val classParser = ClassParser(classFilePath)
    val clazz = classParser.parse()

    val bitScheduler = BitsSchedulerImpl()
    val classSat = ClassSat(clazz, bitScheduler)
    val arg1 = bitScheduler.getAndShift(INT_BITS)
    val arg2 = bitScheduler.getAndShift(INT_BITS)

    val methods = classSat.parsedMethods.iterator().asSequence().toList()
    println(methods.last().value.parse(listOf(arg1, arg2)))
}
