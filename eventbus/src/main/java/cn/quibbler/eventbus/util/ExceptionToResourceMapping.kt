package cn.quibbler.eventbus.util

import cn.quibbler.eventbus.Logger
import java.util.logging.Level;

/**
 * Maps throwables to texts for error dialogs. Use Config to configure the mapping.
 *
 * @author Markus
 */
class ExceptionToResourceMapping(val throwableToMsgIdMap: HashMap<Class<out Throwable?>, Int> = HashMap()) {

    /** Looks at the exception and its causes trying to find an ID. */
    fun mapThrowable(throwable: Throwable): Int? {
        var throwableToCheck: Throwable? = throwable

        var depthToGo = 20
        while (true) {
            val resId: Int? = mapThrowableFlat(throwableToCheck)
            if (resId != null) {
                return resId
            } else {
                throwableToCheck = throwableToCheck?.cause
                depthToGo--
                if (depthToGo <= 0 || throwableToCheck == throwable || throwableToCheck == null) {
                    val logger = Logger.Default.get()
                    logger.log(Level.FINE, "No specific message resource ID found for $throwable")
                    // return config.defaultErrorMsgId;
                    return null
                }
            }
        }
    }

    /** Mapping without checking the cause (done in mapThrowable). */
    fun mapThrowableFlat(throwable: Throwable?): Int? {
        val throwableClass: Class<out Throwable?>? = throwable?.javaClass
        var resId: Int? = throwableToMsgIdMap[throwableClass]
        if (resId == null) {
            var closestClass: Class<out Throwable>? = null
            val mappings = throwableToMsgIdMap.entries
            for (mapping in mappings) {
                val candidate = mapping.key
                if (closestClass == null || closestClass.isAssignableFrom(candidate)) {
                    closestClass = candidate
                    resId = mapping.value
                }
            }
        }
        return resId
    }

    fun addMapping(clazz: Class<out Throwable?>, msgId: Int): ExceptionToResourceMapping {
        throwableToMsgIdMap[clazz] = msgId
        return this
    }

}