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
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

class Cli : CliktCommand(name = "transbyte") {
    val classFilePaths: List<String> by argument(
        name = "files",
        help = "All classes for the translator. " +
            "You can also pass .java files, and transbyte will try to compile them using system Java compiler"
    ).multiple(required = true)

    val startClass: String by option(
        names = arrayOf("--start-class"),
        help = "Class name where to find start method"
    ).required()

    val methodStartName: String? by option(
        names = arrayOf("--method"),
        help = "Name of the method to start translation with. " +
            "If class has only one method, this method will be taken"
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
            val classFiles = classFilePaths.filter { s -> !s.endsWith(".java") }.toMutableList()
            val javaFiles = classFilePaths.filter { s -> s.endsWith(".java") }

            if (javaFiles.isNotEmpty()) {
                val compiler = ToolProvider.getSystemJavaCompiler()
                if (compiler == null) {
                    logger.debug("Can't find system Java compiler!")
                    throw RuntimeException("Can't compile .java files: could not find system Java compiler")
                }

                val diagnostics = DiagnosticCollector<JavaFileObject>()
                val fileManager = compiler.getStandardFileManager(diagnostics, null, null)
                val compilationUnits = fileManager.getJavaFileObjectsFromStrings(javaFiles)

                val task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits)
                val isSuccess = task.call()
                if (!isSuccess) {
                    val errorMsg = StringBuilder()

                    errorMsg.append("Can't compile .java files:\n")
                    for (d in diagnostics.diagnostics) {
                        errorMsg.append(d.toString())
                        errorMsg.append("\n")
                    }

                    throw RuntimeException(errorMsg.toString())
                }

                javaFiles.forEach {
                    classFiles.add(
                        it.dropLast(5) + ".class"
                    )
                }
            }

            val classes = classFiles.map {
                val classParser = ClassParser(it)
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
