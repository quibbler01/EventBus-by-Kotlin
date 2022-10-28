package cn.quibbler.EventBus.android

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object AndroidDependenciesDetector {

    private const val ANDROID_COMPONENTS_IMPLEMENTATION_CLASS_NAME =
        "org.greenrobot.eventbus.android.AndroidComponentsImpl"

    fun isAndroidSDKAvailable(): Boolean {
        try {
            val looperClass = Class.forName("android.os.Looper")
            val getMainLooper: Method = looperClass.getDeclaredMethod("getMainLooper")
            val mainLooper: Any? = getMainLooper.invoke(null)
            return mainLooper != null
        } catch (ignored: ClassNotFoundException) {
        } catch (ignored: NoSuchMethodException) {
        } catch (ignored: IllegalAccessException) {
        } catch (ignored: InvocationTargetException) {
        }

        return false
    }

    fun areAndroidComponentsAvailable(): Boolean {
        try {
            Class.forName(ANDROID_COMPONENTS_IMPLEMENTATION_CLASS_NAME)
            return true
        } catch (ignored: ClassNotFoundException) {
            return false
        }
    }


    fun instantiateAndroidComponents(): AndroidComponents? {
        try {
            val impl = Class.forName(ANDROID_COMPONENTS_IMPLEMENTATION_CLASS_NAME)
            return impl.getConstructor().newInstance() as AndroidComponents
        } catch (ignored: Throwable) {
            return null
        }
    }

}