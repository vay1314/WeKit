package dev.ujhhgtg.wekit.hooks.api.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.ContextMenu
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.wekit.core.dsl.dexMethod
import dev.ujhhgtg.wekit.core.model.ApiHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.utils.annotation.HookItem
import dev.ujhhgtg.wekit.utils.logging.WeLogger
import org.json.JSONObject
import org.luckypray.dexkit.DexKitBridge
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList

@SuppressLint("StaticFieldLeak")
@HookItem(path = "API/视频号分享菜单扩展", desc = "为视频号分享菜单提供添加菜单项功能")
object WeShortVideosShareMenuApi : ApiHookItem(), IResolvesDex {

    interface IMenuItemsProvider {
        fun getMenuItems(param: XC_MethodHook.MethodHookParam): List<MenuItem>
    }

    data class MenuItem(
        val id: Int,
        val text: String, val drawable: () -> Drawable,
        val onClick: (XC_MethodHook.MethodHookParam, Int, List<JSONObject>) -> Unit
    )

    private const val TAG: String = "WeShortVideosShareMenuApi"

    private val providers = CopyOnWriteArrayList<IMenuItemsProvider>()

    fun addProvider(provider: IMenuItemsProvider) {
        if (!providers.contains(provider)) {
            providers.add(provider)
        }
    }

    fun removeProvider(provider: IMenuItemsProvider) {
        val removed = providers.remove(provider)
        WeLogger.i(
            TAG,
            "provider remove ${if (removed) "succeeded" else "failed"}, current provider count: ${providers.size}"
        )
    }

    private val methodCreateMenu1 by dexMethod()
    private val methodOnSelectMenuItem1 by dexMethod()
    private val methodCreateMenu2 by dexMethod()
    private val methodOnSelectMenuItem2 by dexMethod()

    override fun onEnable() {
        methodCreateMenu1.hookBefore { param ->
            val menu = param.args[0] as ContextMenu
            handleCreateMenu(param, menu)
        }

        methodOnSelectMenuItem1.hookBefore { param ->
            val menuItem = param.args[0] as android.view.MenuItem
            val baseFinderFeed = param.thisObject.asResolver()
                .firstField {
                    type = "com.tencent.mm.plugin.finder.model.BaseFinderFeed"
                }
                .get()!!
            handleOnSelectMenuItem(param, menuItem, baseFinderFeed)
        }

        methodCreateMenu2.hookBefore { param ->
            val menu = param.args[1] as ContextMenu
            handleCreateMenu(param, menu)
        }

        methodOnSelectMenuItem2.hookBefore { param ->
            val menuItem = param.args[1] as android.view.MenuItem
            val baseFinderFeed = param.args[0]
            handleOnSelectMenuItem(param, menuItem, baseFinderFeed)
        }
    }

    private fun handleCreateMenu(
        param: XC_MethodHook.MethodHookParam,
        menu: ContextMenu
    ) {
        for (provider in providers) {
            try {
                for (item in provider.getMenuItems(param)) {
                    menu.asResolver()
                        .firstMethod {
                            parameters(Int::class, CharSequence::class, Drawable::class)
                        }
                        .invoke(item.id, item.text, item.drawable())
                }
            } catch (ex: Exception) {
                WeLogger.e(
                    TAG,
                    "provider ${provider.javaClass.name} threw while providing menu items",
                    ex
                )
            }
        }
    }

    private fun handleOnSelectMenuItem(
        param: XC_MethodHook.MethodHookParam,
        menuItem: android.view.MenuItem,
        baseFinderFeed: Any
    ) {
        val itemId = menuItem.itemId
        val finderItem = baseFinderFeed.asResolver()
            .firstField {
                name = "feedObject"
                superclass()
            }
            .get()!!
        val mediaType = finderItem.asResolver()
            .firstMethod {
                name = "getMediaType"
            }
            .invoke()!! as Int
        val mediaList = finderItem.asResolver()
            .firstMethod {
                name = "getMediaList"
            }
            .invoke() as LinkedList<*>
        val mediaJsonList = mediaList.map { media ->
            media.asResolver()
                .firstMethod {
                    name = "toJSON"
                    superclass()
                }.invoke()!! as JSONObject
        }

        for (provider in providers) {
            try {
                for (item in provider.getMenuItems(param)) {
                    if (item.id == itemId) {
                        item.onClick(param, mediaType, mediaJsonList)
                        param.result = null
                        return
                    }
                }
            } catch (ex: Exception) {
                WeLogger.e(
                    TAG,
                    "provider ${provider.javaClass.name} threw while handling click event",
                    ex
                )
            }
        }
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodCreateMenu1.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                name = "onCreateMMMenu"
                usingEqStrings("pos is error ")
            }
        }

        methodOnSelectMenuItem1.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                name = "onMMMenuItemSelected"
                usingEqStrings("[getMoreMenuItemSelectedListener] feed ")
            }
        }

        methodCreateMenu2.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                usingEqStrings("feed", "menu", "sheet", "holder", "KEY_FINDER_SELF_FLAG")
            }
        }

        methodOnSelectMenuItem2.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.plugin.finder.feed")
            matcher {
                declaredClass {
                    usingEqStrings("Finder.FinderLoaderFeedUIContract.Presenter")
                }

                usingEqStrings("getMoreMenuItemSelectedListener feed ")
            }
        }

        return descriptors
    }
}
