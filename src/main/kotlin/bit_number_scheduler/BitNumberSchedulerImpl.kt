package bit_number_scheduler

class BitNumberScheduler {
    private var currentPosition: Long = 0L

    fun shiftPosition(size: Int): Long {
        val curPosition = currentPosition
        currentPosition += size

        return curPosition
    }
}