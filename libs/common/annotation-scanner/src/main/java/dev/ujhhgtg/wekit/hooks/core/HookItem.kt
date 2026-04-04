package dev.ujhhgtg.wekit.hooks.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HookItem(
    val path: String,
    val description: String = ""
)
