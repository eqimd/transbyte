package constants

import bit_scheduler.BitScheduler

object GlobalSettings {
    private lateinit var _bitScheduler: BitScheduler
    val bitScheduler: BitScheduler
        get() = _bitScheduler

    fun setupSettings(
        bitScheduler: BitScheduler,
    ) {
        _bitScheduler = bitScheduler
    }
}
