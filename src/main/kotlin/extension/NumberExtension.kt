package extension

import exception.NotSupportedOperationOnNumbersException

operator fun Number.plus(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() + constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() + constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() + constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() + constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't add ${this.javaClass} and ${constant.javaClass}")
        }
    }

operator fun Number.times(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() * constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() * constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() * constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() * constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't multiply ${this.javaClass} and ${constant.javaClass}")
        }
    }

operator fun Number.minus(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() - constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() - constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() - constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() - constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't subtract ${this.javaClass} and ${constant.javaClass}")
        }
    }

infix fun Number.xor(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() xor constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() xor constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() xor constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() xor constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't 'xor' ${this.javaClass} and ${constant.javaClass}")
        }
    }

infix fun Number.or(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() or constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() or constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() or constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() or constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't 'or' ${this.javaClass} and ${constant.javaClass}")
        }
    }

infix fun Number.and(constant: Number): Number =
    when {
        (this is Byte && constant is Byte) -> {
            this.toByte() and constant.toByte()
        }
        (this is Short && constant is Short) -> {
            this.toShort() and constant.toShort()
        }
        (this is Int && constant is Int) -> {
            this.toInt() and constant.toInt()
        }
        (this is Long && constant is Long) -> {
            this.toLong() and constant.toLong()
        }
        else -> {
            throw NotSupportedOperationOnNumbersException("Can't 'and' ${this.javaClass} and ${constant.javaClass}")
        }
    }
