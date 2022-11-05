package bits_field

data class BitsField(val bitsPosition: Long, val size: Int) {
    constructor() : this(-1, -1)
}