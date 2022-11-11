package bit_number_scheduler

import boolean_formula.Bit
import constants.BitsArray

class BitsSchedulerImpl(private var currentPosition: Long) : BitsScheduler {
    constructor() : this(0)

    override fun getAndShift(size: Int): BitsArray {
        val curPosition = currentPosition
        currentPosition += size

        return Array(size) { Bit(curPosition + it) }
    }
}
