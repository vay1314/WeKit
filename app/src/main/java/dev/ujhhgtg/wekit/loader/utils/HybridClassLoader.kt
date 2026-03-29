package dev.ujhhgtg.wekit.loader.utils

import android.content.Context

object HybridClassLoader : ClassLoader(Context::class.java.classLoader) {

    private val bootClassLoader: ClassLoader = Context::class.java.classLoader!!
    lateinit var moduleParentClassLoader: ClassLoader
    lateinit var hostClassLoader: ClassLoader

    override fun findClass(name: String): Class<*> {
        try {
            return bootClassLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
        }

        try {
            return moduleParentClassLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
        }

        try {
            return hostClassLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
        }

        throw ClassNotFoundException(name)
    }
}
