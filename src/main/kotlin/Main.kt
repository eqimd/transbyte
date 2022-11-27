import bit_number_scheduler.BitsSchedulerImpl
import boolean_logic.data.BitsArrayWithNumber
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_types.ClassSat

fun main(args: Array<String>) {
    val classFilePath = "docs/examples/Sum/S.class"
    val classParser = ClassParser(classFilePath)
    val clazz = classParser.parse()

    val bitScheduler = BitsSchedulerImpl()
    val classSat = ClassSat(clazz, bitScheduler)
    val arg1 = BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS))
    val arg2 = BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS))

    val f = classSat.getMethodByDescription("sum:(II)I")!!
    val retVal = f.parse(listOf(arg1, arg2))
    println(retVal.system)
    println(retVal.returnPrimitive?.constant)
}
