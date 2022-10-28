package cn.quibbler.EventBus.android

import cn.quibbler.EventBus.Logger
import cn.quibbler.EventBus.MainThreadSupport


open class AndroidComponents(val logger: Logger, val defaultMainThreadSupport: MainThreadSupport) {

    companion object {
        private val implementation: AndroidComponents? =
            if (AndroidDependenciesDetector.isAndroidSDKAvailable()) {
                AndroidDependenciesDetector.instantiateAndroidComponents()
            } else null


        fun areAvailable(): Boolean = implementation != null

        fun get(): AndroidComponents? = implementation

    }

}