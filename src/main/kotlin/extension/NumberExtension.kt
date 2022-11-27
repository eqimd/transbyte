package extension

import exception.NotSupportedOperationOnNumbersException

operator fun Number.plus(constant: Number): Number =
    when {
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
