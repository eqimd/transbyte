package constants

import bit_scheduler.BitScheduler

object GlobalSettings {
    private lateinit var _bitScheduler: BitScheduler
    val bitScheduler: BitScheduler
        get() = _bitScheduler

    private lateinit var _arraySizes: List<Int>
    val arraySizes: List<Int>
        get() = _arraySizes

    fun setupSettings(
        bitScheduler: BitScheduler,
        arraySizes: List<Int> = emptyList(),
    ) {
        _bitScheduler = bitScheduler
        _arraySizes = arraySizes
    }
}
