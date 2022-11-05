package cn.quibbler.EventBus

import cn.quibbler.EventBus.android.AndroidComponents
import java.util.logging.Level

interface Logger {

    fun log(level: Level, msg: String)

    fun log(level: Level, msg: String, th: Throwable?)

    class JavaLogger(tag: String) : Logger {

        protected val logger: java.util.logging.Logger = java.util.logging.Logger.getLogger(tag)

        override fun log(level: Level, msg: String) {
            logger.log(level, msg)
        }

        override fun log(level: Level, msg: String, th: Throwable?) {
            logger.log(level, msg, th)
        }
    }

    class SystemOutLogger : Logger {
        override fun log(level: Level, msg: String) {
            System.out.println("[$level]$msg")
        }

        override fun log(level: Level, msg: String, th: Throwable?) {
            System.out.println("[$level]$msg")
            th?.printStackTrace(System.out)
        }
    }

    class Default {

        companion object {

            fun get(): Logger {
                if (AndroidComponents.areAvailable()) {
                    return AndroidComponents.get()!!.logger
                }

                return SystemOutLogger()
            }

        }

    }

}