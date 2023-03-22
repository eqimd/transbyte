package bit_scheduler

import constants.BitsArray

interface BitScheduler {
    val currentPosition: Long

    fun getAndShift(size: Int): BitsArray
}
