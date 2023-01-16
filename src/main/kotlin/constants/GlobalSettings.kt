package constants

import bit_scheduler.BitScheduler
import parsed_types.data.VariableSat

object GlobalSettings {
    private lateinit var _bitScheduler: BitScheduler
    private lateinit var _primitiveConstants: HashMap<Number, VariableSat.Primitive>

    val bitScheduler: BitScheduler
        get() = _bitScheduler

    val primitiveConstants: HashMap<Number, VariableSat.Primitive>
        get() = _primitiveConstants
    fun setupSettings(bitScheduler: BitScheduler, primitiveConstants: HashMap<Number, VariableSat.Primitive>) {
        _bitScheduler = bitScheduler
        _primitiveConstants = primitiveConstants
    }
}
