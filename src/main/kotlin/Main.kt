import bit_scheduler.BitSchedulerImpl
import constants.Constants
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required
import org.apache.bcel.classfile.ClassParser
import org.slf4j.simple.SimpleLogger
import translator.BytecodeTranslatorImpl

fun main(args: Array<String>) {
    val argParser = ArgParser(Constants.PROGRAM_NAME)

    val isDebug by argParser.option(
        ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Turn on debug info"
    ).default(false)

    val classFilePaths by argParser.option(
        ArgType.String,
        shortName = "c",
        fullName = "classes",
        description = "All paths to classes for the translator"
    ).multiple()

    val startClass by argParser.option(
        ArgType.String,
        shortName = "s",
        fullName = "start-class",
        description = "Class name where to find start method"
    ).required()

    val methodStartName by argParser.option(
        ArgType.String,
        shortName = "m",
        fullName = "method",
        description = "Name of the method to start translation with"
    ).default(Constants.TRANSLATOR_START_FUNCTION_NAME)

    argParser.parse(args)

    if (isDebug) {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, Constants.LOG_LEVEL_DEBUG)
    }
    val classes = Array(classFilePaths.size) { i ->
        val classParser = ClassParser(classFilePaths[i])
        classParser.parse()
    }

    val bitScheduler = BitSchedulerImpl()
    val translator = BytecodeTranslatorImpl(*classes, bitScheduler = bitScheduler)

    val circuit = translator.translate(startClass, methodStartName)

    for (eq in circuit.system) {
        println(eq)
    }
}
