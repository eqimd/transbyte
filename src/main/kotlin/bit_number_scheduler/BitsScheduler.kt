package bit_number_scheduler

import constants.BitsArray

interface BitsScheduler {
    fun getAndShift(size: Int): BitsArray
}
