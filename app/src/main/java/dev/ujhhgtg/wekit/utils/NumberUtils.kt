package dev.ujhhgtg.wekit.utils

fun Long.coerceToInt(): Int {
    return this.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
