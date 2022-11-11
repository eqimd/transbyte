package bit_number_scheduler

import extension.BitsArray

interface BitNumberScheduler {
    fun getAndShift(size: Int): BitsArray
}