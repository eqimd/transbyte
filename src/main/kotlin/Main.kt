
import constants.Constants
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required
import mu.KotlinLogging
import org.apache.bcel.classfile.ClassParser
import org.slf4j.simple.SimpleLogger
import translator.BytecodeTranslatorImpl
import java.io.PrintStream

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

    val saveFilename by argParser.option(
        ArgType.String,
        fullName = "output",
        description = "Filename for output"
    )

    val arraySizes by argParser.option(
        ArgType.Int,
        fullName = "array-sizes",
        description = "Array sizes for input in method"
    ).multiple()

    argParser.parse(args)

    if (isDebug) {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, Constants.LOG_LEVEL_DEBUG)
    }
    val classes = List(classFilePaths.size) { i ->
        val classParser = ClassParser(classFilePaths[i])
        classParser.parse()
    }

    val translator = BytecodeTranslatorImpl(classes, arraySizes)

    val circuit = translator.translate(startClass, methodStartName)
    val outStream = if (saveFilename == null) System.out else PrintStream(saveFilename!!)
    circuit.saveInAigerFormat(outStream)

    val logger = KotlinLogging.logger {}
    for (eq in circuit.system) {
        logger.debug { eq }
    }
}
