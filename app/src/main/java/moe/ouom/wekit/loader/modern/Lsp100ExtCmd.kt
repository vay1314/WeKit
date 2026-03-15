package moe.ouom.wekit.loader.modern

import io.github.libxposed.api.XposedInterface
import moe.ouom.wekit.loader.ModuleLoader
import moe.ouom.wekit.loader.hookimpl.LibXposedApiByteCodeGenerator
import moe.ouom.wekit.loader.modern.codegen.Lsp100ProxyClassMaker
import java.lang.reflect.Method

object Lsp100ExtCmd {

    fun handleQueryExtension(cmd: String, arg: Array<Any?>?): Any? {
        return when (cmd) {
            "GetXposedInterfaceClass" -> XposedInterface::class.java
            "GetLoadPackageParam" -> null
            "GetInitZygoteStartupParam" -> null
            "GetInitErrors" -> ModuleLoader.initErrors
            LibXposedApiByteCodeGenerator.CMD_SET_WRAPPER -> {
                Lsp100ProxyClassMaker.setWrapperMethod(checkNotNull(arg)[0] as Method)
                true
            }
            else -> null
        }
    }
}
