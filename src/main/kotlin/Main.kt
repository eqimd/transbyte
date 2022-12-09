import bit_scheduler.BitSchedulerImpl
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.Variable

fun main(args: Array<String>) {
    val classFilePath = "docs/examples/Sum/S.class"
    val classParser = ClassParser(classFilePath)
    val clazz = classParser.parse()

    val bitScheduler = BitSchedulerImpl()
    val classSat = ClassSat(clazz, bitScheduler)
    val arg1 = Variable.BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS), 1)
    val arg2 = Variable.BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS), 1)

    val f = classSat.getMethodByDescription("sum:(II)I")!!
    val retVal = f.parse(arg1, arg2) as MethodSat.MethodParseReturnValue.SystemWithPrimitive
    println(retVal.system)
    println(retVal.primitive.constant)
}
