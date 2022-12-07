package translator

import bit_scheduler.BitSchedulerImpl
import boolean_logic.BooleanFormula
import org.apache.bcel.classfile.JavaClass

class BytecodeTranslatorImpl(vararg classes: JavaClass) : Translator {
    private val classesMap = HashMap<String, JavaClass>()
    private val bitScheduler = BitSchedulerImpl()

    init {
        for (clazz in classes.iterator()) {
            classesMap[clazz.className] = clazz
        }
    }

    override fun translate(): BooleanFormula {
        TODO("")
    }
}
