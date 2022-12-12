import bit_scheduler.BitSchedulerImpl
import constants.Constants.BOOLEAN_BITS
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_types.ClassSat
import parsed_types.MethodSat
import parsed_types.data.Variable

fun main(args: Array<String>) {
    val classFilePath = "docs/examples/LFSR/LFSR.class"
    val classParser = ClassParser(classFilePath)
    val clazz = classParser.parse()

    val bitScheduler = BitSchedulerImpl()
    val classSat = ClassSat(clazz, bitScheduler)
    val arg1 = Variable.BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS))
    val arg2 = Variable.BitsArrayWithNumber(bitScheduler.getAndShift(INT_BITS))

//    val f = classSat.getMethodByDescription("sum:(II)I")!!
//    val retVal = f.parse(arg1, arg2) as MethodSat.MethodParseReturnValue.SystemWithPrimitive
    val bitArr = Variable.ArrayReference.ArrayPrimitives(100, BOOLEAN_BITS, bitScheduler)
    val f = classSat.getMethodByDescription("get_lfsr:([Z)[Z")!!
    val retVal = f.parse(bitArr) as MethodSat.MethodParseReturnValue.SystemOnly
    for (sys in retVal.system) {
        println()
        for (eq in sys) {
            println(eq)
        }
    }
//    println(retVal.primitive.constant)
}
