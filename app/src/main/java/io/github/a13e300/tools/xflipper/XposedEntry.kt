package io.github.a13e300.tools.xflipper

import android.app.Application
import android.content.Context
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import com.facebook.soloader.nativeloader.NativeLoader
import com.facebook.soloader.nativeloader.SystemDelegate
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.a13e300.tools.xflipper.bridge.BridgeSocketImpl
import java.net.Socket
import java.net.SocketImpl

class XposedEntry : IXposedHookZygoteInit {
    companion object {
        lateinit var appContext: Context
    }
    private var mInitialized: Boolean = false


    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XposedBridge.hookAllMethods(
            Class.forName("com.facebook.flipper.android.DelegatingSocketFactory"),
            "createSocket",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val socket = param.result as Socket
                    val impl = XposedHelpers.getObjectField(socket, "impl") as? SocketImpl
                    Logger.d("socket=$socket impl=$impl")
                    XposedHelpers.setObjectField(socket, "impl", BridgeSocketImpl(socket, impl))
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            Application::class.java, "attach",
            Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.d("after attach " + param.thisObject)
                    val context = param.thisObject as Application
                    initializeFlipper(context)
                }
            })
    }

    private fun initializeFlipper(context: Context) {
        synchronized(this) {
            if (mInitialized) return
            appContext = context
            runCatching {
                SoLoader::class.java.getDeclaredField("isEnabled").apply {
                    isAccessible = true
                    set(null, false)
                }
                NativeLoader.init(SystemDelegate())
                val client = AndroidFlipperClient.getInstance(context)
                val dm = DescriptorMapping.withDefaults()
                client.addPlugin(InspectorFlipperPlugin(context, dm))
                client.start()
                mInitialized = true
            }.onFailure {
                Logger.e("failed to initialize flipper", it)
            }.onSuccess {
                Logger.d("flipper initialize success")
            }
        }
    }
}