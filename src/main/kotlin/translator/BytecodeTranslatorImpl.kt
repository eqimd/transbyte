package translator

import bit_number_scheduler.BitsSchedulerImpl
import boolean_formula.BooleanFormula
import org.apache.bcel.classfile.JavaClass

class BytecodeTranslatorImpl(vararg classes: JavaClass) : Translator {
    private val classesMap = HashMap<String, JavaClass>()
    private val bitScheduler = BitsSchedulerImpl()

    init {
        for (clazz in classes.iterator()) {
            classesMap[clazz.className] = clazz
        }
    }

    override fun translate(): BooleanFormula {

        TODO("")
    }
}
