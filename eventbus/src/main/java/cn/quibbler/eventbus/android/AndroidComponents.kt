package cn.quibbler.eventbus.android

import cn.quibbler.eventbus.Logger
import cn.quibbler.eventbus.MainThreadSupport


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