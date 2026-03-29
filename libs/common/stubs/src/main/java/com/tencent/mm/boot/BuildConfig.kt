package com.tencent.mm.boot

object BuildConfig {

    // we use a kotlin `object` and no `const` here to prevent inlining
    @Suppress("MayBeConstant")
    @JvmField
    val BUILD_TAG: String = "Stub!"
}
