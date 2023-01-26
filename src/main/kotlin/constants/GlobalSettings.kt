package constants

import bit_scheduler.BitScheduler
import parsed_types.data.VariableSat

object GlobalSettings {
    private lateinit var _bitScheduler: BitScheduler
    val bitScheduler: BitScheduler
        get() = _bitScheduler

    private lateinit var _primitiveConstants: HashMap<Number, VariableSat.Primitive>
    val primitiveConstants: HashMap<Number, VariableSat.Primitive>
        get() = _primitiveConstants

    private lateinit var _arraySizes: List<Int>
    val arraySizes: List<Int>
        get() = _arraySizes

    fun setupSettings(
        bitScheduler: BitScheduler,
        primitiveConstants: HashMap<Number, VariableSat.Primitive>,
        arraySizes: List<Int> = emptyList(),
    ) {
        _bitScheduler = bitScheduler
        _primitiveConstants = primitiveConstants
        _arraySizes = arraySizes
    }
}
