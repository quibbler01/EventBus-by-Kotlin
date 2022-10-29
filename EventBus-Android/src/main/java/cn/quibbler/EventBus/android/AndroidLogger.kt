package cn.quibbler.EventBus.android

import android.util.Log
import cn.quibbler.EventBus.Logger
import java.util.logging.Level

class AndroidLogger(private val tag: String) : Logger {

    override fun log(level: Level, msg: String) {
        if (level != Level.OFF) {
            Log.println(mapLevel(level), tag, msg)
        }
    }

    override fun log(level: Level, msg: String, th: Throwable?) {
        if (level != Level.OFF) {
            // That's how Log does it internally
            Log.println(mapLevel(level), tag, "$msg \n ${Log.getStackTraceString(th)}")
        }
    }

    private fun mapLevel(level: Level): Int {
        val value = level.intValue()
        return if (value < 800) { // below INFO
            if (value < 500) { // below FINE
                Log.VERBOSE
            } else {
                Log.DEBUG
            }
        } else if (value < 900) { // below WARNING
            Log.INFO
        } else if (value < 1000) { // below ERROR
            Log.WARN
        } else {
            Log.ERROR
        }
    }

}