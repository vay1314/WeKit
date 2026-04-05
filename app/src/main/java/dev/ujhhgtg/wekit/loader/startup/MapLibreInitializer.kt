package dev.ujhhgtg.wekit.loader.startup

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import dev.ujhhgtg.wekit.utils.ModuleRes
import org.maplibre.android.MapLibre

object MapLibreInitializer {

    fun init(hostContext: Context) {
        val wrapper = ContextWrapper1(ModuleRes.moduleContext, ContextWrapper2(ModuleRes.moduleContext.resources, hostContext))
        MapLibre.getInstance(wrapper)
    }
}

// ok this is even more painful than osmdroid
private class ContextWrapper1(moduleContext: Context,
                              val hostContext: Context): ContextWrapper(moduleContext) {

    override fun getApplicationContext(): Context {
        return hostContext
    }
}

private class ContextWrapper2(val mResources: Resources,
                              val hostContext: Context): ContextWrapper(hostContext) {

    override fun getResources(): Resources {
        return mResources
    }

    override fun getAssets(): AssetManager? {
        return mResources.assets
    }

    override fun getTheme(): Resources.Theme {
        return mResources.newTheme()
    }

    override fun getApplicationContext(): Context {
        return hostContext
    }
}
