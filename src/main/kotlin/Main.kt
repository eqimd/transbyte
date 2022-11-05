import bit_number_scheduler.BitNumberSchedulerImpl
import bits_field.BitsField
import constants.Constants.INT_BITS
import org.apache.bcel.classfile.ClassParser
import parsed_types.MethodSat

fun main(args: Array<String>) {
    val classFile = "docs/examples/Sum/S.class"
    val classParser = ClassParser(classFile)
    val cls = classParser.parse()

//    println("FIELDS:")
//    for (field in cls.fields) {
//        println(field.name)
//    }

    val mtd = cls.methods.find { it.name == "sum" }
    val bitScheduler = BitNumberSchedulerImpl()
    val classSat = MethodSat(cls, mtd!!, bitScheduler)
    val arg1 = BitsField(bitScheduler.shift(INT_BITS), INT_BITS)
    val arg2 = BitsField(bitScheduler.shift(INT_BITS), INT_BITS)
    val parsed = classSat.parse(listOf(arg1, arg2))
    for (expr in parsed) {
        println(expr)
    }

//    println("\nMETHODS:")
//    for (mtd in cls.methods) {
//        val mg = MethodGen(mtd, cls.className, ConstantPoolGen(cls.constantPool))
//        println("\t" + mtd)
//        for (instr in mg.instructionList) {
//            println("\t\t" + instr.instruction.name)
//        }
//    }
}
