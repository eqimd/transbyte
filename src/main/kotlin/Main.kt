import bit_number_scheduler.BitsSchedulerImpl
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_types.ClassSat

fun main(args: Array<String>) {
    val classFilePath = "docs/examples/Invoke/Invoke.class"
    val classParser = ClassParser(classFilePath)
    val clazz = classParser.parse()

    val bitScheduler = BitsSchedulerImpl()
    val classSat = ClassSat(clazz, bitScheduler)
    val arg1 = bitScheduler.getAndShift(INT_BITS)
    val arg2 = bitScheduler.getAndShift(INT_BITS)

    val f = classSat.getMethodByDescription("inv1:()V")!!
    val system = f.parse(emptyList())
    println(system)
}
