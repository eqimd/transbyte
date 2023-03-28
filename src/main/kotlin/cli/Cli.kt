package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.int
import constants.Constants
import mu.KotlinLogging
import org.apache.bcel.classfile.ClassParser
import org.slf4j.simple.SimpleLogger
import translator.BytecodeTranslatorImpl
import java.io.PrintStream

class Cli : CliktCommand() {
    val classFilePaths: List<String> by argument(
        name = "classes",
        help = "All classes for the translator"
    ).multiple(required = true)

    val startClass: String by option(
        names = arrayOf("--start-class"),
        help = "Class name where to find start method"
    ).required()

    val methodStartName: String? by option(
        names = arrayOf("--method"),
        help = "Name of the method to start translation with. " +
            "If class has only one method, this method will be taken automatically"
    )

    val arraySizes: List<Int> by option(
        names = arrayOf("--array-sizes"),
        help = "Array sizes for input method, separated by ','"
    ).int().split(",").default(emptyList())

    val saveFilename: String? by option(
        names = arrayOf("-o", "--output"),
        help = "Filename for output"
    )

    val isDebug: Boolean by option(
        names = arrayOf("-d", "--debug"),
        help = "Turn on debug info"
    ).flag(default = false)

    override fun run() {
        if (isDebug) {
            System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, Constants.LOG_LEVEL_DEBUG)
        }

        val logger = KotlinLogging.logger {}

        try {
            val classes = List(classFilePaths.size) { i ->
                val classParser = ClassParser(classFilePaths[i])
                classParser.parse()
            }

            val translator = BytecodeTranslatorImpl(classes, arraySizes)

            val circuit = translator.translate(startClass, methodStartName)

            for (eq in circuit.system) {
                logger.debug(eq.toString())
            }

            val outStream = if (saveFilename == null) System.out else PrintStream(saveFilename!!)
            circuit.saveInAigerFormat(outStream)
        } catch (e: Exception) {
            logger.debug(e.message)
            logger.debug("Stacktrace:")
            e.stackTrace.forEach {
                logger.debug("\t$it")
            }

            System.err.println(e.message)
        }
    }
}
