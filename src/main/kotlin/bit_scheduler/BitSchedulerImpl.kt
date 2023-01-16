package bit_scheduler

import boolean_logic.BooleanFormula
import constants.BitsArray

class BitSchedulerImpl(private var _currentPosition: Long = 0) : BitScheduler {
    override val currentPosition: Long
        get() = _currentPosition

    override fun getAndShift(size: Int): BitsArray {
        val curPosition = currentPosition
        _currentPosition += size

        return Array(size) { BooleanFormula.Variable.Bit(curPosition + it) }
    }
}
