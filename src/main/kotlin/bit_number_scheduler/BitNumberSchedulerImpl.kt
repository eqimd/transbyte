package bit_number_scheduler

class BitNumberSchedulerImpl(private var currentPosition: Long) : BitNumberScheduler {
    constructor() : this(0)

    override fun shift(size: Int): Long {
        val curPosition = currentPosition
        currentPosition += size

        return curPosition
    }
}