package moe.ouom.wekit.loader.modern

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import moe.ouom.wekit.loader.hookapi.IClassLoaderHelper
import moe.ouom.wekit.loader.hookapi.IHookBridge
import moe.ouom.wekit.loader.hookapi.ILoaderService
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method

class Lsp100HookImpl private constructor() : IHookBridge, ILoaderService {

    private var mClassLoaderHelper: IClassLoaderHelper? = null

    override fun getApiLevel(): Int = XposedInterface.API

    override fun getFrameworkName(): String = self!!.frameworkName

    override fun getFrameworkVersion(): String = self!!.frameworkVersion

    override fun getFrameworkVersionCode(): Long = self!!.frameworkVersionCode

    override fun hookMethod(member: Member, callback: IHookBridge.IMemberHookCallback, priority: Int): IHookBridge.MemberUnhookHandle =
        Lsp100HookWrapper.hookAndRegisterMethodCallback(member, callback, priority)

    override fun isDeoptimizationSupported(): Boolean = true

    override fun deoptimize(member: Member): Boolean {
        return when (member) {
            is Method -> self!!.deoptimize(member)
            is Constructor<*> -> self!!.deoptimize(member)
            else -> throw IllegalArgumentException("only method and constructor can be deoptimized")
        }
    }

    @Throws(NullPointerException::class, IllegalAccessException::class, IllegalArgumentException::class, InvocationTargetException::class)
    override fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any? {
        return self!!.invokeOrigin(method, thisObject, args)
    }

    @Throws(InvocationTargetException::class, IllegalArgumentException::class, IllegalAccessException::class, InstantiationException::class)
    override fun <T : Any> newInstanceOrigin(ctor: Constructor<T>, vararg args: Any?): T {
        return self!!.newInstanceOrigin(ctor, args)
    }

    @Throws(NullPointerException::class, IllegalAccessException::class, IllegalArgumentException::class, InvocationTargetException::class)
    override fun <T> invokeOriginalConstructor(ctor: Constructor<T>, thisObject: T, args: Array<Any?>) {
        checkNotNull(thisObject)
        self!!.invokeOrigin(ctor, thisObject, args)
    }

    @Suppress("UNCHECKED_CAST")
    override fun queryExtension(key: String, vararg args: Any?): Any? {
        return Lsp100ExtCmd.handleQueryExtension(
            key,
            (args as Array<Any?>?).takeIf { args.isNotEmpty() })
    }

    override fun log(msg: String) { self!!.log(msg) }

    override fun log(tr: Throwable) { self!!.log(tr.toString(), tr) }

    override fun getClassLoaderHelper(): IClassLoaderHelper? = mClassLoaderHelper

    override fun setClassLoaderHelper(helper: IClassLoaderHelper?) { mClassLoaderHelper = helper }

    override fun getHookCounter(): Long = Lsp100HookWrapper.getHookCounter().toLong()

    companion object {
        @JvmField
        val INSTANCE = Lsp100HookImpl()

        @JvmField
        var self: XposedModule? = null

        @JvmStatic
        fun init(base: XposedModule) {
            self = base
            Lsp100HookWrapper.self = base
        }
    }
}
