package bit_scheduler

import constants.BitsArray

interface BitScheduler {
    fun getAndShift(size: Int): BitsArray
}
