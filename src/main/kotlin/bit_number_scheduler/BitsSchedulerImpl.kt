package bit_number_scheduler

import boolean_formula.Bit
import extension.BitsArray

class BitNumberSchedulerImpl(private var currentPosition: Long) : BitNumberScheduler {
    constructor() : this(0)

    override fun getAndShift(size: Int): BitsArray {
        val curPosition = currentPosition
        currentPosition += size

        return Array(size) { Bit(curPosition + it) }
    }
}