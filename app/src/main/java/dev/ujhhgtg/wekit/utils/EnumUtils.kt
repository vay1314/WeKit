package dev.ujhhgtg.wekit.utils

@Suppress("UNCHECKED_CAST")
fun enumValueOfClass(enumClass: Class<*>, name: String): Enum<*> {
    return java.lang.Enum.valueOf(enumClass as Class<out Enum<*>?>, name)
}
