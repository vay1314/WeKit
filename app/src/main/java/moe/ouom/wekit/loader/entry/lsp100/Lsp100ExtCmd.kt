package moe.ouom.wekit.loader.entry.lsp100

import io.github.libxposed.api.XposedInterface
import moe.ouom.wekit.loader.entry.lsp100.codegen.Lsp100ProxyClassMaker
import moe.ouom.wekit.loader.utils.LibXposedApiByteCodeGenerator
import java.lang.Boolean
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.Throwable

object Lsp100ExtCmd {
    fun handleQueryExtension(cmd: String, arg: Array<Any?>?): Any? {
        when (cmd) {
            "GetXposedInterfaceClass" -> return XposedInterface::class.java
            "GetInitErrors" -> return emptyList<Throwable?>()
            LibXposedApiByteCodeGenerator.CMD_SET_WRAPPER -> {
                Lsp100ProxyClassMaker.setWrapperMethod((arg!![0] as java.lang.reflect.Method?)!!)
                return Boolean.TRUE
            }
            "GetLoadPackageParam", "GetInitZygoteStartupParam" -> return null
            else -> return null
        }
    }
}
