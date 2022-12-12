package bit_scheduler

import boolean_logic.base.BitVariable
import constants.BitsArray

class BitSchedulerImpl(private var currentPosition: Long) : BitScheduler {
    constructor() : this(0)

    override fun getAndShift(size: Int): BitsArray {
        val curPosition = currentPosition
        currentPosition += size

        return Array(size) { BitVariable(curPosition + it) }
    }
}
