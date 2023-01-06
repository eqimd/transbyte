package bit_scheduler

import boolean_logic.BooleanFormula
import constants.BitsArray

class BitSchedulerImpl(private var currentPosition: Long = 0) : BitScheduler {

    override fun getAndShift(size: Int): BitsArray {
        val curPosition = currentPosition
        currentPosition += size

        return Array(size) { BooleanFormula.Variable.Bit(curPosition + it) }
    }
}
